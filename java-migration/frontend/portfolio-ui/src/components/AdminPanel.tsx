import { useState, useEffect } from 'react';
import { Settings, User, Shield, RefreshCw, Save, AlertCircle, CheckCircle } from 'lucide-react';
import { portfolioApi } from '../api';
import type { Portfolio, PortfolioUpdateRequest } from '../types';

function AdminPanel() {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [selectedPortfolio, setSelectedPortfolio] = useState<Portfolio | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [updateForm, setUpdateForm] = useState({
    status: '',
    clientName: '',
    totalValue: '',
    userId: 'ADMIN',
  });

  const [newPortfolioForm, setNewPortfolioForm] = useState({
    portfolioId: '',
    accountNo: '',
    clientName: '',
    clientType: 'INDIVIDUAL',
    userId: 'ADMIN',
  });

  const [activeTab, setActiveTab] = useState<'update' | 'create'>('update');

  useEffect(() => {
    loadPortfolios();
  }, []);

  const loadPortfolios = async () => {
    try {
      setLoading(true);
      const data = await portfolioApi.getAll();
      setPortfolios(data);
    } catch (err) {
      setError('Failed to load portfolios');
      console.error('Error loading portfolios:', err);
    } finally {
      setLoading(false);
    }
  };

  const handlePortfolioSelect = (portfolioId: string) => {
    const portfolio = portfolios.find(p => p.portfolioId === portfolioId);
    setSelectedPortfolio(portfolio || null);
    if (portfolio) {
      setUpdateForm({
        status: portfolio.status,
        clientName: portfolio.clientName,
        totalValue: portfolio.totalValue?.toString() || '0',
        userId: 'ADMIN',
      });
    }
    setError(null);
    setSuccess(null);
  };

  const handleUpdateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPortfolio) return;

    setError(null);
    setSuccess(null);

    try {
      setSaving(true);
      const updateData: PortfolioUpdateRequest = {
        userId: updateForm.userId,
      };

      if (updateForm.status !== selectedPortfolio.status) {
        updateData.status = updateForm.status as 'ACTIVE' | 'CLOSED' | 'SUSPENDED';
      }
      if (updateForm.clientName !== selectedPortfolio.clientName) {
        updateData.clientName = updateForm.clientName;
      }
      if (parseFloat(updateForm.totalValue) !== selectedPortfolio.totalValue) {
        updateData.totalValue = parseFloat(updateForm.totalValue);
      }

      await portfolioApi.update(selectedPortfolio.portfolioId, updateData);
      setSuccess('Portfolio updated successfully');
      await loadPortfolios();
    } catch (err) {
      setError('Failed to update portfolio');
      console.error('Error updating portfolio:', err);
    } finally {
      setSaving(false);
    }
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!newPortfolioForm.portfolioId || !newPortfolioForm.accountNo || !newPortfolioForm.clientName) {
      setError('Please fill in all required fields');
      return;
    }

    try {
      setSaving(true);
      await portfolioApi.create(newPortfolioForm);
      setSuccess('Portfolio created successfully');
      setNewPortfolioForm({
        portfolioId: '',
        accountNo: '',
        clientName: '',
        clientType: 'INDIVIDUAL',
        userId: 'ADMIN',
      });
      await loadPortfolios();
    } catch (err) {
      setError('Failed to create portfolio');
      console.error('Error creating portfolio:', err);
    } finally {
      setSaving(false);
    }
  };

  const handleStatusUpdate = async (status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED') => {
    if (!selectedPortfolio) return;

    try {
      setSaving(true);
      await portfolioApi.updateStatus(selectedPortfolio.portfolioId, status, updateForm.userId);
      setSuccess(`Portfolio status updated to ${status}`);
      await loadPortfolios();
      handlePortfolioSelect(selectedPortfolio.portfolioId);
    } catch (err) {
      setError('Failed to update status');
      console.error('Error updating status:', err);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[#CBD5E1]">Loading admin panel...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-white">Administrative Panel</h1>
        <p className="mt-1 text-[#94A3B8]">Manage portfolio settings and administrative updates</p>
      </div>

      {error && (
        <div className="bg-[#F87171]/10 border border-[#F87171]/30 rounded-xl p-4 flex items-center">
          <AlertCircle className="h-5 w-5 text-[#F87171] mr-3 flex-shrink-0" />
          <span className="text-[#F87171]">{error}</span>
        </div>
      )}

      {success && (
        <div className="bg-[#4ADE80]/10 border border-[#4ADE80]/30 rounded-xl p-4 flex items-center">
          <CheckCircle className="h-5 w-5 text-[#4ADE80] mr-3 flex-shrink-0" />
          <span className="text-[#4ADE80]">{success}</span>
        </div>
      )}

      <div className="bg-[#1E293B] rounded-xl border border-[#334155]">
        <div className="border-b border-[#334155]">
          <nav className="flex">
            <button
              onClick={() => setActiveTab('update')}
              className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'update'
                  ? 'border-[#22D3EE] text-[#22D3EE]'
                  : 'border-transparent text-[#94A3B8] hover:text-white'
              }`}
            >
              <Settings className="h-4 w-4 inline mr-2" />
              Update Portfolio
            </button>
            <button
              onClick={() => setActiveTab('create')}
              className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'create'
                  ? 'border-[#22D3EE] text-[#22D3EE]'
                  : 'border-transparent text-[#94A3B8] hover:text-white'
              }`}
            >
              <User className="h-4 w-4 inline mr-2" />
              Create Portfolio
            </button>
          </nav>
        </div>

        <div className="p-6">
          {activeTab === 'update' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div className="lg:col-span-1">
                <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                  Select Portfolio
                </label>
                <select
                  value={selectedPortfolio?.portfolioId || ''}
                  onChange={(e) => handlePortfolioSelect(e.target.value)}
                  className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
                >
                  <option value="">Choose a portfolio</option>
                  {portfolios.map(p => (
                    <option key={p.portfolioId} value={p.portfolioId}>
                      {p.portfolioId} - {p.clientName}
                    </option>
                  ))}
                </select>

                {selectedPortfolio && (
                  <div className="mt-4 p-4 bg-[#0F172A] rounded-lg border border-[#334155]">
                    <h4 className="text-sm font-medium text-white mb-3">Current Status</h4>
                    <div className="space-y-2 text-sm">
                      <div className="flex justify-between">
                        <span className="text-[#94A3B8]">Status</span>
                        <span className={`font-medium ${
                          selectedPortfolio.status === 'ACTIVE' ? 'text-[#4ADE80]' :
                          selectedPortfolio.status === 'SUSPENDED' ? 'text-[#FBBF24]' :
                          'text-[#F87171]'
                        }`}>{selectedPortfolio.status}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-[#94A3B8]">Total Value</span>
                        <span className="text-white">${selectedPortfolio.totalValue?.toLocaleString('en-US', { minimumFractionDigits: 2 })}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-[#94A3B8]">Last Updated</span>
                        <span className="text-white">{selectedPortfolio.lastMaintDate || 'N/A'}</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              <div className="lg:col-span-2">
                {selectedPortfolio ? (
                  <form onSubmit={handleUpdateSubmit} className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                          Status (S)
                        </label>
                        <select
                          value={updateForm.status}
                          onChange={(e) => setUpdateForm(prev => ({ ...prev, status: e.target.value }))}
                          className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
                        >
                          <option value="ACTIVE">Active</option>
                          <option value="SUSPENDED">Suspended</option>
                          <option value="CLOSED">Closed</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                          Admin User ID
                        </label>
                        <input
                          type="text"
                          value={updateForm.userId}
                          onChange={(e) => setUpdateForm(prev => ({ ...prev, userId: e.target.value }))}
                          className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
                        />
                      </div>

                      <div className="md:col-span-2">
                        <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                          Client Name (N)
                        </label>
                        <input
                          type="text"
                          value={updateForm.clientName}
                          onChange={(e) => setUpdateForm(prev => ({ ...prev, clientName: e.target.value }))}
                          className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
                        />
                      </div>

                      <div className="md:col-span-2">
                        <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                          Total Value (V)
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          value={updateForm.totalValue}
                          onChange={(e) => setUpdateForm(prev => ({ ...prev, totalValue: e.target.value }))}
                          className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
                        />
                      </div>
                    </div>

                    <div className="flex gap-4">
                      <button
                        type="submit"
                        disabled={saving}
                        className="flex items-center px-6 py-2.5 bg-[#22D3EE] text-[#0F172A] rounded-lg font-medium hover:bg-[#22D3EE]/90 transition-colors disabled:opacity-50"
                      >
                        <Save className="h-4 w-4 mr-2" />
                        {saving ? 'Saving...' : 'Save Changes'}
                      </button>
                      <button
                        type="button"
                        onClick={() => handlePortfolioSelect(selectedPortfolio.portfolioId)}
                        className="flex items-center px-6 py-2.5 bg-[#334155] text-white rounded-lg font-medium hover:bg-[#475569] transition-colors"
                      >
                        <RefreshCw className="h-4 w-4 mr-2" />
                        Reset
                      </button>
                    </div>

                    <div className="border-t border-[#334155] pt-6">
                      <h4 className="text-sm font-medium text-white mb-4">Quick Status Actions</h4>
                      <div className="flex gap-3">
                        <button
                          type="button"
                          onClick={() => handleStatusUpdate('ACTIVE')}
                          disabled={saving || selectedPortfolio.status === 'ACTIVE'}
                          className="px-4 py-2 bg-[#4ADE80]/20 text-[#4ADE80] rounded-lg font-medium hover:bg-[#4ADE80]/30 transition-colors disabled:opacity-50"
                        >
                          Activate
                        </button>
                        <button
                          type="button"
                          onClick={() => handleStatusUpdate('SUSPENDED')}
                          disabled={saving || selectedPortfolio.status === 'SUSPENDED'}
                          className="px-4 py-2 bg-[#FBBF24]/20 text-[#FBBF24] rounded-lg font-medium hover:bg-[#FBBF24]/30 transition-colors disabled:opacity-50"
                        >
                          Suspend
                        </button>
                        <button
                          type="button"
                          onClick={() => handleStatusUpdate('CLOSED')}
                          disabled={saving || selectedPortfolio.status === 'CLOSED'}
                          className="px-4 py-2 bg-[#F87171]/20 text-[#F87171] rounded-lg font-medium hover:bg-[#F87171]/30 transition-colors disabled:opacity-50"
                        >
                          Close
                        </button>
                      </div>
                    </div>
                  </form>
                ) : (
                  <div className="flex items-center justify-center h-64 text-[#94A3B8]">
                    Select a portfolio to view and update its settings
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === 'create' && (
            <form onSubmit={handleCreateSubmit} className="max-w-xl space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                    Portfolio ID *
                  </label>
                  <input
                    type="text"
                    value={newPortfolioForm.portfolioId}
                    onChange={(e) => setNewPortfolioForm(prev => ({ ...prev, portfolioId: e.target.value.toUpperCase() }))}
                    placeholder="e.g., PORT006"
                    className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                    Account Number *
                  </label>
                  <input
                    type="text"
                    value={newPortfolioForm.accountNo}
                    onChange={(e) => setNewPortfolioForm(prev => ({ ...prev, accountNo: e.target.value }))}
                    placeholder="e.g., ACC006"
                    className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]"
                  />
                </div>

                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                    Client Name *
                  </label>
                  <input
                    type="text"
                    value={newPortfolioForm.clientName}
                    onChange={(e) => setNewPortfolioForm(prev => ({ ...prev, clientName: e.target.value }))}
                    placeholder="Enter client name"
                    className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                    Client Type
                  </label>
                  <select
                    value={newPortfolioForm.clientType}
                    onChange={(e) => setNewPortfolioForm(prev => ({ ...prev, clientType: e.target.value }))}
                    className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
                  >
                    <option value="INDIVIDUAL">Individual</option>
                    <option value="CORPORATE">Corporate</option>
                    <option value="TRUST">Trust</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#CBD5E1] mb-2">
                    Created By
                  </label>
                  <input
                    type="text"
                    value={newPortfolioForm.userId}
                    onChange={(e) => setNewPortfolioForm(prev => ({ ...prev, userId: e.target.value }))}
                    className="w-full px-4 py-2.5 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={saving}
                className="flex items-center px-6 py-2.5 bg-[#22D3EE] text-[#0F172A] rounded-lg font-medium hover:bg-[#22D3EE]/90 transition-colors disabled:opacity-50"
              >
                <Shield className="h-4 w-4 mr-2" />
                {saving ? 'Creating...' : 'Create Portfolio'}
              </button>
            </form>
          )}
        </div>
      </div>

      <div className="bg-[#1E293B] rounded-xl border border-[#334155] p-6">
        <h3 className="text-lg font-medium text-white mb-4">Administrative Update Types</h3>
        <p className="text-[#CBD5E1] text-sm mb-4">
          Based on the COBOL PORTUPDT.cbl program, the following update types are supported:
        </p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="p-4 bg-[#0F172A] rounded-lg border border-[#334155]">
            <div className="flex items-center mb-2">
              <span className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-[#22D3EE]/20 text-[#22D3EE] font-bold mr-3">S</span>
              <span className="text-white font-medium">Status Update</span>
            </div>
            <p className="text-[#94A3B8] text-sm">Change portfolio status (Active, Suspended, Closed)</p>
          </div>
          <div className="p-4 bg-[#0F172A] rounded-lg border border-[#334155]">
            <div className="flex items-center mb-2">
              <span className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-[#60A5FA]/20 text-[#60A5FA] font-bold mr-3">N</span>
              <span className="text-white font-medium">Name Update</span>
            </div>
            <p className="text-[#94A3B8] text-sm">Update client name associated with portfolio</p>
          </div>
          <div className="p-4 bg-[#0F172A] rounded-lg border border-[#334155]">
            <div className="flex items-center mb-2">
              <span className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-[#818CF8]/20 text-[#818CF8] font-bold mr-3">V</span>
              <span className="text-white font-medium">Value Update</span>
            </div>
            <p className="text-[#94A3B8] text-sm">Manually adjust total portfolio value</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default AdminPanel;
