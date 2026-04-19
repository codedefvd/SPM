import { useEffect, useMemo, useState } from "react";
import { adminDictionaryApi } from "../../api/adminDictionary";
import { StatCard } from "../../components/StatCard";

const DICT_TYPE_OPTIONS = ["CATEGORY", "STATUS", "BUSINESS_CODE"];

const EMPTY_DICTIONARY_FORM = {
  dictType: "CATEGORY",
  dictCode: "",
  dictName: "",
  businessValue: "",
  sortOrder: 0,
  enabled: true,
  description: "",
};

export function AdminDictionariesPage({ workspace }) {
  const [dictionaries, setDictionaries] = useState([]);
  const [dictTypeFilter, setDictTypeFilter] = useState("");
  const [dictionaryForm, setDictionaryForm] = useState(EMPTY_DICTIONARY_FORM);
  const [editingDictionaryId, setEditingDictionaryId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function loadDictionaries() {
    setLoading(true);
    setError("");
    try {
      const data = await adminDictionaryApi.listDictionaries(workspace?.token, dictTypeFilter || undefined);
      setDictionaries(data || []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load dictionaries");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDictionaries();
  }, [dictTypeFilter]);

  async function handleSaveDictionary() {
    setSaving(true);
    setError("");
    setMessage("");
    try {
      const payload = {
        ...dictionaryForm,
        sortOrder: Number(dictionaryForm.sortOrder),
      };

      if (editingDictionaryId) {
        await adminDictionaryApi.updateDictionary(workspace?.token, editingDictionaryId, {
          dictName: payload.dictName,
          businessValue: payload.businessValue,
          sortOrder: payload.sortOrder,
          enabled: payload.enabled,
          description: payload.description,
        });
        setMessage(`Dictionary #${editingDictionaryId} updated.`);
      } else {
        await adminDictionaryApi.createDictionary(workspace?.token, payload);
        setMessage("Dictionary entry created.");
      }

      setDictionaryForm(EMPTY_DICTIONARY_FORM);
      setEditingDictionaryId(null);
      await loadDictionaries();
    } catch (requestError) {
      setError(requestError.message || "Failed to save dictionary entry");
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteDictionary(dictionaryId) {
    if (!window.confirm(`Delete dictionary #${dictionaryId}?`)) {
      return;
    }
    setSaving(true);
    setError("");
    setMessage("");
    try {
      await adminDictionaryApi.deleteDictionary(workspace?.token, dictionaryId);
      setMessage(`Dictionary #${dictionaryId} deleted.`);
      await loadDictionaries();
    } catch (requestError) {
      setError(requestError.message || "Failed to delete dictionary entry");
    } finally {
      setSaving(false);
    }
  }

  function handleStartEdit(item) {
    setEditingDictionaryId(item.dictionaryId);
    setDictionaryForm({
      dictType: item.dictType || "CATEGORY",
      dictCode: item.dictCode || "",
      dictName: item.dictName || "",
      businessValue: item.businessValue || "",
      sortOrder: item.sortOrder ?? 0,
      enabled: item.enabled ?? true,
      description: item.description || "",
    });
  }

  const dictionaryStats = useMemo(() => {
    return {
      total: dictionaries.length,
      enabled: dictionaries.filter((item) => item.enabled).length,
      typeCount: new Set(dictionaries.map((item) => item.dictType)).size,
    };
  }, [dictionaries]);

  return (
    <div className="page-stack">
      <section className="page-card">
        <div className="section-head">
          <div>
            <span className="eyebrow">Admin Story A4</span>
            <h2 className="section-title">Data Dictionary Workspace</h2>
          </div>
          <p className="page-note">
            Maintain category, status, and business code dictionaries so downstream modules can reuse unified values.
          </p>
        </div>

        <div className="stats-grid">
          <StatCard label="Dictionary Entries" value={dictionaryStats.total} />
          <StatCard label="Enabled Entries" value={dictionaryStats.enabled} />
          <StatCard label="Dictionary Types" value={dictionaryStats.typeCount} />
        </div>
      </section>

      <section className="page-card split-grid">
        <div>
          <h3 className="section-title">{editingDictionaryId ? `Edit Dictionary #${editingDictionaryId}` : "Create Dictionary Entry"}</h3>
          <div className="form-grid">
            <select
              value={dictionaryForm.dictType}
              onChange={(event) => setDictionaryForm((prev) => ({ ...prev, dictType: event.target.value }))}
              disabled={Boolean(editingDictionaryId)}
            >
              {DICT_TYPE_OPTIONS.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
            <input
              placeholder="Dictionary Code"
              value={dictionaryForm.dictCode}
              onChange={(event) => setDictionaryForm((prev) => ({ ...prev, dictCode: event.target.value }))}
              disabled={Boolean(editingDictionaryId)}
            />
            <input
              placeholder="Dictionary Name"
              value={dictionaryForm.dictName}
              onChange={(event) => setDictionaryForm((prev) => ({ ...prev, dictName: event.target.value }))}
            />
            <input
              placeholder="Business Value"
              value={dictionaryForm.businessValue}
              onChange={(event) => setDictionaryForm((prev) => ({ ...prev, businessValue: event.target.value }))}
            />
            <input
              type="number"
              min={0}
              placeholder="Sort Order"
              value={dictionaryForm.sortOrder}
              onChange={(event) => setDictionaryForm((prev) => ({ ...prev, sortOrder: event.target.value }))}
            />
            <select
              value={dictionaryForm.enabled ? "true" : "false"}
              onChange={(event) => setDictionaryForm((prev) => ({ ...prev, enabled: event.target.value === "true" }))}
            >
              <option value="true">Enabled</option>
              <option value="false">Disabled</option>
            </select>
            <textarea
              className="span-2"
              placeholder="Description"
              value={dictionaryForm.description}
              onChange={(event) => setDictionaryForm((prev) => ({ ...prev, description: event.target.value }))}
            />
          </div>
          <div className="inline-actions">
            <button className="primary-button" type="button" disabled={saving} onClick={handleSaveDictionary}>
              {saving ? "Saving..." : editingDictionaryId ? "Update Entry" : "Create Entry"}
            </button>
            {editingDictionaryId ? (
              <button
                className="secondary-button"
                type="button"
                onClick={() => {
                  setEditingDictionaryId(null);
                  setDictionaryForm(EMPTY_DICTIONARY_FORM);
                }}
              >
                Cancel Edit
              </button>
            ) : null}
          </div>
          {message ? <p className="page-note">{message}</p> : null}
          {error ? <p className="page-note">{error}</p> : null}
        </div>

        <div>
          <h3 className="section-title">Dictionary Entries</h3>
          <div className="toolbar">
            <select value={dictTypeFilter} onChange={(event) => setDictTypeFilter(event.target.value)}>
              <option value="">All Types</option>
              {DICT_TYPE_OPTIONS.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
            <button className="secondary-button" type="button" onClick={loadDictionaries}>
              Refresh
            </button>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Business</th>
                  <th>Sort</th>
                  <th>Enabled</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {dictionaries.map((item) => (
                  <tr key={item.dictionaryId}>
                    <td>{item.dictionaryId}</td>
                    <td>{item.dictType}</td>
                    <td>{item.dictCode}</td>
                    <td>{item.dictName}</td>
                    <td>{item.businessValue || "-"}</td>
                    <td>{item.sortOrder ?? 0}</td>
                    <td>{String(item.enabled)}</td>
                    <td>
                      <div className="table-actions">
                        <button className="primary-button" type="button" onClick={() => handleStartEdit(item)}>
                          Edit
                        </button>
                        <button
                          className="secondary-button"
                          type="button"
                          disabled={saving}
                          onClick={() => handleDeleteDictionary(item.dictionaryId)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!loading && dictionaries.length === 0 ? (
                  <tr>
                    <td colSpan="8">No dictionary entries.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  );
}
