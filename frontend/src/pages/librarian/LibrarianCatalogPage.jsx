import { useEffect, useMemo, useState } from "react";
import { librarianApi } from "../../api/client";
import { StatCard } from "../../components/StatCard";
import Barcode from "../../components/Barcode";

const EMPTY_BOOK_FORM = {
  title: "",
  author: "",
  isbn: "",
  barcode: "",
  categoryId: "",
  publisher: "",
  description: "",
  thumbnailUrl: "",
  publishedDate: "",
  storageLocation: "",
  totalCopies: 1,
  availableCopies: 1,
  shelfStatus: "ON_SHELF",
};

const EMPTY_CATEGORY_FORM = {
  code: "",
  name: "",
  enabled: true,
};

export function LibrarianCatalogPage({ workspace }) {
  const permissionsLoaded = Array.isArray(workspace?.permissions);
  const permissions = permissionsLoaded ? workspace.permissions : [];
  const canManageBooks = !permissionsLoaded || permissions.includes("BOOK_MANAGE");
  const canManageInventory = !permissionsLoaded || permissions.includes("INVENTORY_MANAGE");
  const hasCatalogAccess = canManageBooks || canManageInventory;

  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([]);
  const [bookForm, setBookForm] = useState(EMPTY_BOOK_FORM);
  const [categoryForm, setCategoryForm] = useState(EMPTY_CATEGORY_FORM);
  const [editingCategoryId, setEditingCategoryId] = useState(null);
  const [editingBookId, setEditingBookId] = useState(null);
  const [editingInventoryOnly, setEditingInventoryOnly] = useState(false);
  const [selectedCopyBook, setSelectedCopyBook] = useState(null);
  const [bookCopies, setBookCopies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isbnLoading, setIsbnLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function loadData() {
    if (!hasCatalogAccess) {
      setBooks([]);
      setCategories([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const requests = [librarianApi.listBooks(workspace?.token)];
      if (canManageBooks) {
        requests.push(librarianApi.listCategories(workspace?.token));
      }

      const [bookList, categoryList = []] = await Promise.all(requests);
      setBooks(bookList || []);
      setCategories(categoryList || []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load catalog data");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, [workspace?.token, hasCatalogAccess, canManageBooks]);

  async function handleSaveBook() {
    if (!editingBookId && !canManageBooks) {
      setError("Current librarian role cannot create books.");
      setMessage("");
      return;
    }

    const validationMessage = validateBookForm(bookForm, canManageBooks, Boolean(editingBookId));
    if (validationMessage) {
      setError(validationMessage);
      setMessage("");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");
    try {
      const payload = {
        ...bookForm,
        categoryId: canManageBooks ? Number(bookForm.categoryId) : undefined,
        totalCopies: Number(bookForm.totalCopies),
        availableCopies: Number(bookForm.availableCopies),
      };

      if (editingBookId) {
        if (canManageBooks && !editingInventoryOnly) {
          await librarianApi.updateBook(workspace?.token, editingBookId, payload);
        }
        if (canManageInventory) {
          await librarianApi.updateInventory(workspace?.token, editingBookId, {
            totalCopies: payload.totalCopies,
            availableCopies: payload.availableCopies,
          });
          await librarianApi.updateShelfStatus(workspace?.token, editingBookId, {
            shelfStatus: payload.shelfStatus,
          });
        }
        setMessage(`Book #${editingBookId} updated.`);
      } else {
        await librarianApi.createBook(workspace?.token, payload);
        setMessage("Book created.");
      }

      resetBookForm();
      await loadData();
    } catch (requestError) {
      setError(requestError.message || "Failed to save book");
    } finally {
      setSaving(false);
    }
  }

  async function handleLookupIsbn() {
    if (!canManageBooks) {
      setError("Current librarian role cannot query ISBN metadata.");
      setMessage("");
      return;
    }
    if (!bookForm.isbn?.trim()) {
      setError("Please enter ISBN first.");
      setMessage("");
      return;
    }

    setIsbnLoading(true);
    setError("");
    setMessage("");
    try {
      const result = await librarianApi.lookupBookByIsbn(workspace?.token, bookForm.isbn.trim());
      const matchedCategory = categories.find((item) => {
        const categoryName = (result.categoryName || "").toLowerCase();
        return categoryName && (
          item.name?.toLowerCase() === categoryName
          || item.code?.toLowerCase() === categoryName
          || categoryName.includes(item.name?.toLowerCase() || "")
        );
      });

      setBookForm((prev) => ({
        ...prev,
        title: result.title || prev.title,
        author: result.author || prev.author,
        publisher: result.publisher || prev.publisher,
        description: result.description || prev.description,
        publishedDate: result.publishedDate || prev.publishedDate,
        thumbnailUrl: result.thumbnailUrl || prev.thumbnailUrl,
        categoryId: matchedCategory ? String(matchedCategory.categoryId) : prev.categoryId,
      }));
      setMessage("ISBN metadata loaded.");
    } catch (requestError) {
      setError(requestError.message || "Failed to lookup ISBN metadata");
    } finally {
      setIsbnLoading(false);
    }
  }

  async function handleSaveCategory() {
    if (!canManageBooks) {
      setError("Current librarian role cannot manage categories.");
      setMessage("");
      return;
    }

    const validationMessage = validateCategoryForm(categoryForm, editingCategoryId);
    if (validationMessage) {
      setError(validationMessage);
      setMessage("");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");
    try {
      if (editingCategoryId) {
        await librarianApi.updateCategory(workspace?.token, editingCategoryId, {
          name: categoryForm.name,
          enabled: categoryForm.enabled,
        });
        setMessage(`Category #${editingCategoryId} updated.`);
      } else {
        await librarianApi.createCategory(workspace?.token, categoryForm);
        setMessage("Category created.");
      }
      resetCategoryForm();
      await loadData();
    } catch (requestError) {
      setError(requestError.message || "Failed to save category");
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteBook(bookId) {
    if (!canManageBooks || !window.confirm(`Delete book #${bookId}?`)) {
      return;
    }
    setSaving(true);
    setError("");
    setMessage("");
    try {
      await librarianApi.deleteBook(workspace?.token, bookId);
      setMessage(`Book #${bookId} deleted.`);
      await loadData();
    } catch (requestError) {
      setError(requestError.message || "Failed to delete book");
    } finally {
      setSaving(false);
    }
  }

  async function handleUpdateCopyLocation(copyId, storageLocation) {
    if (!canManageInventory || !storageLocation?.trim()) {
      return;
    }
    setSaving(true);
    setError("");
    try {
      await librarianApi.updateCopyLocation(workspace?.token, copyId, { storageLocation: storageLocation.trim() });
      setMessage(`Copy #${copyId} location updated.`);
      if (selectedCopyBook) {
        const result = await librarianApi.listBookCopies(workspace?.token, selectedCopyBook.bookId);
        setBookCopies(result || []);
      }
    } catch (requestError) {
      setError(requestError.message || "Failed to update copy location");
    } finally {
      setSaving(false);
    }
  }

  async function handleLoadBookCopies(book) {
    setError("");
    setMessage("");
    try {
      const result = await librarianApi.listBookCopies(workspace?.token, book.bookId);
      setSelectedCopyBook(book);
      setBookCopies(result || []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load copy barcodes");
    }
  }

  function startEditBook(book) {
    if (!canManageBooks) {
      return;
    }
    setEditingBookId(book.bookId);
    setEditingInventoryOnly(false);
    setBookForm(toBookForm(book));
  }

  function startAdjustInventory(book) {
    if (!canManageInventory) {
      return;
    }
    setEditingBookId(book.bookId);
    setEditingInventoryOnly(true);
    setBookForm(toBookForm(book));
  }

  function handleStartEditCategory(category) {
    if (!canManageBooks) {
      return;
    }
    setEditingCategoryId(category.categoryId);
    setCategoryForm({
      code: category.code || "",
      name: category.name || "",
      enabled: Boolean(category.enabled),
    });
  }

  async function handleDeleteCategory(categoryId) {
    if (!canManageBooks || !window.confirm(`Delete category #${categoryId}?`)) {
      return;
    }
    setSaving(true);
    setError("");
    setMessage("");
    try {
      await librarianApi.deleteCategory(workspace?.token, categoryId, { force: true });
      if (editingCategoryId === categoryId) {
        resetCategoryForm();
      }
      setMessage(`Category #${categoryId} deleted.`);
      await loadData();
    } catch (requestError) {
      setError(requestError.message || "Failed to delete category");
    } finally {
      setSaving(false);
    }
  }

  function resetBookForm() {
    setEditingBookId(null);
    setEditingInventoryOnly(false);
    setBookForm(EMPTY_BOOK_FORM);
  }

  function resetCategoryForm() {
    setEditingCategoryId(null);
    setCategoryForm(EMPTY_CATEGORY_FORM);
  }

  const stats = useMemo(() => ({
    total: books.length,
    available: books.reduce((sum, book) => sum + (book.availableCopies || 0), 0),
    offShelf: books.filter((book) => book.shelfStatus === "OFF_SHELF").length,
  }), [books]);

  const selectableCategories = useMemo(() => {
    const enabledCategories = categories.filter((item) => item.enabled);
    if (!editingBookId) {
      return enabledCategories;
    }

    const currentCategory = categories.find((item) => String(item.categoryId) === String(bookForm.categoryId));
    if (currentCategory && !currentCategory.enabled) {
      return [currentCategory, ...enabledCategories.filter((item) => item.categoryId !== currentCategory.categoryId)];
    }
    return enabledCategories;
  }, [categories, editingBookId, bookForm.categoryId]);

  const disableMetadataFields = Boolean(editingBookId) && editingInventoryOnly;

  return (
    <div className="page-stack">
      <section className="page-card">
        <div className="section-head compact">
          <div>
            <span className="eyebrow">Librarian</span>
            <h2 className="section-title">Catalog</h2>
          </div>
        </div>
        <div className="stats-grid">
          <StatCard label="Catalog Items" value={stats.total} />
          <StatCard label="Available Copies" value={stats.available} />
          <StatCard label="Off Shelf" value={stats.offShelf} />
        </div>
      </section>

      {!hasCatalogAccess ? (
        <section className="page-card">
          <p className="page-note">No catalog access.</p>
        </section>
      ) : (
        <>
          <section className="page-card">
            <h3 className="section-title">
              {!canManageBooks && canManageInventory
                ? editingBookId
                  ? `Adjust Inventory #${editingBookId}`
                  : "Adjust Inventory"
                : editingBookId
                  ? `Edit Book #${editingBookId}`
                  : "Create Book"}
            </h3>
            {!canManageBooks && canManageInventory && !editingBookId ? (
              <p className="page-note">Select a book from the table to update copies or shelf status.</p>
            ) : (
              <>
                <div className="form-grid">
                  <div className="field-inline">
                    <input placeholder="ISBN" value={bookForm.isbn} disabled={disableMetadataFields} onChange={(e) => setBookForm((prev) => ({ ...prev, isbn: e.target.value }))} />
                    <button className="secondary-button" type="button" disabled={disableMetadataFields || isbnLoading || saving} onClick={handleLookupIsbn}>
                      {isbnLoading ? "Looking Up..." : "Recognize ISBN"}
                    </button>
                  </div>
                  <input placeholder="Book Title" value={bookForm.title} disabled={disableMetadataFields} onChange={(e) => setBookForm((prev) => ({ ...prev, title: e.target.value }))} />
                  <input placeholder="Author" value={bookForm.author} disabled={disableMetadataFields} onChange={(e) => setBookForm((prev) => ({ ...prev, author: e.target.value }))} />
                  <select value={bookForm.categoryId} disabled={disableMetadataFields} onChange={(e) => setBookForm((prev) => ({ ...prev, categoryId: e.target.value }))}>
                    <option value="">Select Category</option>
                    {selectableCategories.map((item) => (
                      <option key={item.categoryId} value={item.categoryId}>
                        {item.code} - {item.name}{item.enabled ? "" : " (disabled)"}
                      </option>
                    ))}
                  </select>
                  <input placeholder="Barcode" value={bookForm.barcode || "Will be generated automatically"} disabled />
                  <input placeholder="Publisher" value={bookForm.publisher} disabled={disableMetadataFields} onChange={(e) => setBookForm((prev) => ({ ...prev, publisher: e.target.value }))} />
                  <input placeholder="Storage Location (e.g. 3F-A-12)" value={bookForm.storageLocation} disabled={disableMetadataFields} onChange={(e) => setBookForm((prev) => ({ ...prev, storageLocation: e.target.value }))} />
                  <input placeholder="Published Date" value={bookForm.publishedDate} disabled={disableMetadataFields} onChange={(e) => setBookForm((prev) => ({ ...prev, publishedDate: e.target.value }))} />
                  <select value={bookForm.shelfStatus} disabled={!canManageInventory} onChange={(e) => setBookForm((prev) => ({ ...prev, shelfStatus: e.target.value }))}>
                    <option value="ON_SHELF">ON_SHELF</option>
                    <option value="OFF_SHELF">OFF_SHELF</option>
                  </select>
                  <input type="number" placeholder="Total Copies" min={0} disabled={!canManageInventory} value={bookForm.totalCopies} onChange={(e) => setBookForm((prev) => ({ ...prev, totalCopies: e.target.value }))} />
                  <input type="number" placeholder="Available Copies" min={0} disabled={!canManageInventory} value={bookForm.availableCopies} onChange={(e) => setBookForm((prev) => ({ ...prev, availableCopies: e.target.value }))} />
                  <textarea className="span-2" placeholder="Description" value={bookForm.description} disabled={disableMetadataFields} onChange={(e) => setBookForm((prev) => ({ ...prev, description: e.target.value }))} />
                  {bookForm.thumbnailUrl ? (
                    <div>
                      <small>Cover Preview</small>
                      <img src={bookForm.thumbnailUrl} alt="Cover preview" className="cover-thumb-preview" />
                    </div>
                  ) : null}
                </div>
                <div className="inline-actions">
                  <button className="primary-button" type="button" disabled={saving} onClick={handleSaveBook}>
                    {saving ? "Saving..." : editingBookId ? "Save Changes" : "Create Book"}
                  </button>
                  {editingBookId ? <button className="secondary-button" type="button" onClick={resetBookForm}>Cancel Edit</button> : null}
                </div>
              </>
            )}
          </section>

          {canManageBooks ? (
            <section className="page-card split-grid">
              <div>
                <h3 className="section-title">{editingCategoryId ? `Edit Category #${editingCategoryId}` : "Create Category"}</h3>
                <div className="form-grid">
                  <input placeholder="Category Code" value={categoryForm.code} onChange={(e) => setCategoryForm((prev) => ({ ...prev, code: e.target.value }))} disabled={Boolean(editingCategoryId)} />
                  <input placeholder="Category Name" value={categoryForm.name} onChange={(e) => setCategoryForm((prev) => ({ ...prev, name: e.target.value }))} />
                  <select value={String(categoryForm.enabled)} onChange={(e) => setCategoryForm((prev) => ({ ...prev, enabled: e.target.value === "true" }))}>
                    <option value="true">Enabled</option>
                    <option value="false">Disabled</option>
                  </select>
                </div>
                <div className="inline-actions">
                  <button className="secondary-button" type="button" disabled={saving} onClick={handleSaveCategory}>
                    {editingCategoryId ? "Update Category" : "Create Category"}
                  </button>
                  {editingCategoryId ? <button className="secondary-button" type="button" onClick={resetCategoryForm}>Cancel Edit</button> : null}
                </div>
              </div>
              <div>
                <h3 className="section-title">Categories</h3>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Code</th>
                        <th>Name</th>
                        <th>Enabled</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {categories.map((item) => (
                        <tr key={item.categoryId}>
                          <td>{item.categoryId}</td>
                          <td>{item.code}</td>
                          <td>{item.name}</td>
                          <td>{String(item.enabled)}</td>
                          <td>
                            <div className="table-actions">
                              <button className="primary-button" type="button" onClick={() => handleStartEditCategory(item)}>Edit</button>
                              <button className="secondary-button" type="button" onClick={() => handleDeleteCategory(item.categoryId)}>Delete</button>
                            </div>
                          </td>
                        </tr>
                      ))}
                      {!loading && categories.length === 0 ? <tr><td colSpan="5">No categories.</td></tr> : null}
                    </tbody>
                  </table>
                </div>
              </div>
            </section>
          ) : null}

          <section className="page-card">
            {message ? <p className="page-note">{message}</p> : null}
            {error ? <p className="page-note">{error}</p> : null}
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Cover</th>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Author</th>
                    <th>Copy Count</th>
                    <th>Category</th>
                    <th>Location</th>
                    <th>Copies</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {books.map((book) => (
                    <tr key={book.bookId}>
                      <td>
                        {book.thumbnailUrl ? (
                          <img src={book.thumbnailUrl} alt={book.title} className="cover-thumb-table" />
                        ) : (
                          <span className="cover-placeholder">-</span>
                        )}
                      </td>
                      <td>{book.bookId}</td>
                      <td>{book.title}</td>
                      <td>{book.author}</td>
                      <td>{book.totalCopies ?? 0}</td>
                      <td>{book.categoryName || "Uncategorized"}</td>
                      <td>{book.storageLocation || "-"}</td>
                      <td>{book.availableCopies ?? 0}/{book.totalCopies ?? 0}</td>
                      <td>{book.shelfStatus}</td>
                      <td>
                        <div className="table-actions">
                          <button className="secondary-button" type="button" onClick={() => handleLoadBookCopies(book)}>View Copy Barcodes</button>
                          {canManageBooks ? <button className="primary-button" type="button" onClick={() => startEditBook(book)}>Edit</button> : null}
                          {canManageInventory ? <button className="secondary-button" type="button" onClick={() => startAdjustInventory(book)}>Adjust Inventory</button> : null}
                          {canManageBooks ? <button className="secondary-button" type="button" onClick={() => handleDeleteBook(book.bookId)}>Delete</button> : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!loading && books.length === 0 ? <tr><td colSpan="10">No books.</td></tr> : null}
                </tbody>
              </table>
            </div>
          </section>

          {selectedCopyBook ? (
            <section className="page-card">
              <div className="section-head compact">
                <div>
                  <span className="eyebrow">Book Copies</span>
                  <h3 className="section-title">{selectedCopyBook.title}</h3>
                </div>
                <button className="secondary-button" type="button" onClick={() => setSelectedCopyBook(null)}>
                  Close
                </button>
              </div>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Copy No</th>
                      <th>Barcode</th>
                      <th>Storage Location</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bookCopies.map((copy) => (
                      <CopyLocationRow
                        key={copy.copyId}
                        copy={copy}
                        canManageInventory={canManageInventory}
                        saving={saving}
                        onSave={handleUpdateCopyLocation}
                      />
                    ))}
                    {bookCopies.length === 0 ? (
                      <tr>
                        <td colSpan="4">No copy barcodes.</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}
        </>
      )}
    </div>
  );
}

function toBookForm(book) {
  return {
    title: book.title || "",
    author: book.author || "",
    isbn: book.isbn || "",
    barcode: book.barcode || "",
    categoryId: book.categoryId || "",
    publisher: book.publisher || "",
    description: book.description || "",
    thumbnailUrl: book.thumbnailUrl || "",
    publishedDate: book.publishedDate || "",
    storageLocation: book.storageLocation || "",
    totalCopies: book.totalCopies ?? 0,
    availableCopies: book.availableCopies ?? 0,
    shelfStatus: book.shelfStatus || "ON_SHELF",
  };
}

function validateBookForm(bookForm, canManageBooks, isEditing) {
  if (!canManageBooks && !isEditing) {
    return "Current librarian role cannot create books.";
  }
  if (canManageBooks && !bookForm.title?.trim()) {
    return "Title is required.";
  }
  if (canManageBooks && !bookForm.author?.trim()) {
    return "Author is required.";
  }
  if (canManageBooks && !bookForm.isbn?.trim()) {
    return "ISBN is required.";
  }
  if (canManageBooks && !bookForm.categoryId) {
    return "Category is required.";
  }
  if (Number(bookForm.totalCopies) < 0 || Number(bookForm.availableCopies) < 0) {
    return "Copies must be greater than or equal to 0.";
  }
  if (Number(bookForm.availableCopies) > Number(bookForm.totalCopies)) {
    return "Available copies cannot be greater than total copies.";
  }
  return "";
}

function CopyLocationRow({ copy, canManageInventory, saving, onSave }) {
  const [location, setLocation] = useState(copy.storageLocation || "");

  useEffect(() => {
    setLocation(copy.storageLocation || "");
  }, [copy.storageLocation, copy.copyId]);

  return (
    <tr>
      <td>{copy.copyNo}</td>
      <td><Barcode value={copy.barcode} height={30} fontSize={10} displayValue={true} /></td>
      <td>
        {canManageInventory ? (
          <input
            value={location}
            placeholder="e.g. 3F-A-12-05"
            onChange={(event) => setLocation(event.target.value)}
          />
        ) : (
          copy.storageLocation || "-"
        )}
      </td>
      <td>
        {canManageInventory ? (
          <button
            className="secondary-button"
            type="button"
            disabled={saving}
            onClick={() => onSave(copy.copyId, location)}
          >
            Save Location
          </button>
        ) : (
          "-"
        )}
      </td>
    </tr>
  );
}

function validateCategoryForm(categoryForm, editingCategoryId) {
  if (!editingCategoryId && !categoryForm.code?.trim()) {
    return "Category code is required.";
  }
  if (!categoryForm.name?.trim()) {
    return "Category name is required.";
  }
  return "";
}
