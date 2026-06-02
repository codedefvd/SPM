
import { useEffect, useState } from "react";
import { Panel } from "../../components/Panel";
import { librarianApi } from "../../api/librarian";

export function LibrarianNotificationsPage({ workspace }) {
    const [reminders, setReminders] = useState([]);
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(false);

    const loadData = async () => {
        if (!workspace?.token) return;

        setLoading(true);
        try {
            const [remindersData, summaryData] = await Promise.all([
                librarianApi.listReturnReminders(workspace.token),
                librarianApi.getReturnReminderSummary(workspace.token),
            ]);
            setReminders(remindersData || []);
            setSummary(summaryData);
        } catch (error) {
            console.error("Failed to load notification data:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, [workspace?.token]);

    const handleSendReminder = async (recordId) => {
        if (!workspace?.token) return;

        try {
            await librarianApi.sendOverdueReminder(workspace.token, recordId);
            alert("Reminder sent successfully");
        } catch (error) {
            alert("Failed to send: " + (error.message || "Unknown error"));
        }
    };

    const handleSendAllReminders = async () => {
        if (!workspace?.token) return;

        if (!confirm(`Send reminders to all ${reminders.length} overdue records?`)) {
            return;
        }

        try {
            const count = await librarianApi.sendAllOverdueReminders(workspace.token);
            alert(`Successfully sent ${count} reminders`);
        } catch (error) {
            alert("Failed to send: " + (error.message || "Unknown error"));
        }
    };

    return (
        <div className="space-y-4">
            <h1 className="text-2xl font-semibold text-gray-900">Overdue Notifications</h1>

            {summary && (
                <div className="grid grid-cols-3 gap-4">
                    <Panel title="Total Overdue">
                        <p className="text-3xl font-bold text-red-600">{summary.overdueCount || 0}</p>
                    </Panel>
                    <Panel title="Due Today">
                        <p className="text-3xl font-bold text-orange-600">{summary.dueTodayCount || 0}</p>
                    </Panel>
                    <Panel title="Due Soon">
                        <p className="text-3xl font-bold text-yellow-600">{summary.dueSoonCount || 0}</p>
                    </Panel>
                </div>
            )}

            <Panel title="Overdue Records">
                <div className="mb-4 flex justify-between items-center">
                    <p className="text-sm text-gray-600">Total: {reminders.length} records</p>
                    <button
                        onClick={handleSendAllReminders}
                        disabled={reminders.length === 0}
                        className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:bg-gray-400"
                    >
                        Send All Reminders
                    </button>
                </div>

                {loading ? (
                    <p className="text-gray-500">Loading...</p>
                ) : reminders.length === 0 ? (
                    <p className="text-gray-500">No overdue records</p>
                ) : (
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead>
                        <tr>
                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Reader</th>
                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Book</th>
                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Due Date</th>
                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Days Overdue</th>
                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Fine</th>
                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Phone</th>
                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {reminders.map((item) => (
                            <tr key={item.recordId}>
                                <td className="px-4 py-2 text-sm">{item.readerUsername}</td>
                                <td className="px-4 py-2 text-sm">{item.bookTitle}</td>
                                <td className="px-4 py-2 text-sm">{item.dueDate}</td>
                                <td className="px-4 py-2 text-sm text-red-600">{item.overdueDays || 0} days</td>
                                <td className="px-4 py-2 text-sm">${item.fineAmount || '0.00'}</td>
                                <td className="px-4 py-2 text-sm">{item.readerPhone || 'N/A'}</td>
                                <td className="px-4 py-2 text-sm">
                                    <button
                                        onClick={() => handleSendReminder(item.recordId)}
                                        disabled={!item.readerPhone}
                                        className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm disabled:bg-gray-400"
                                    >
                                        Send SMS
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </Panel>
        </div>
    );
}

