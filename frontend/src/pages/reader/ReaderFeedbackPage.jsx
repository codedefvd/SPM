import { useState } from "react";
import { Panel } from "../../components/Panel";

export function ReaderFeedbackPage({ workspace }) {
    const [formData, setFormData] = useState({
        subject: "",
        content: "",
    });

    const handleSubmit = (e) => {
        e.preventDefault();
        alert("Feedback feature is not yet implemented");
    };

    return (
        <div className="space-y-4">
            <h1 className="text-2xl font-semibold text-gray-900">Feedback</h1>

            <Panel title="Submit Feedback">
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Subject
                        </label>
                        <input
                            type="text"
                            value={formData.subject}
                            onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md"
                            placeholder="Enter feedback subject"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Content
                        </label>
                        <textarea
                            value={formData.content}
                            onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                            rows={6}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md"
                            placeholder="Describe your feedback in detail"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                    >
                        Submit Feedback
                    </button>
                </form>
            </Panel>
        </div>
    );
}
