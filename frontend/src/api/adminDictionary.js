import { request } from "./base";

export const adminDictionaryApi = {
  listDictionaries(token, dictType) {
    const query = dictType ? `?dict_type=${encodeURIComponent(dictType)}` : "";
    return request(`/admin/dictionaries${query}`, {}, token);
  },
  createDictionary(token, payload) {
    return request("/admin/dictionaries", { method: "POST", body: JSON.stringify(payload) }, token);
  },
  updateDictionary(token, dictionaryId, payload) {
    return request(`/admin/dictionaries/${dictionaryId}`, { method: "PUT", body: JSON.stringify(payload) }, token);
  },
  deleteDictionary(token, dictionaryId) {
    return request(`/admin/dictionaries/${dictionaryId}`, { method: "DELETE" }, token);
  },
};
