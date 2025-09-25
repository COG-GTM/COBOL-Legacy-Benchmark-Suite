import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  Chip,
  Pagination
} from '@mui/material';
import {
  Search,
  ArrowBack,
  History,
  TrendingUp,
  TrendingDown
} from '@mui/icons-material';
import LoadingSpinner from '../components/LoadingSpinner';

interface Transaction {
  date: string;
  type: 'BUY' | 'SELL' | 'DIV' | 'FEE';
  units: number;
  price: number;
  amount: number;
  description: string;
}

const TransactionHistory: React.FC = () => {
  const navigate = useNavigate();
  const [accountId, setAccountId] = useState('');
  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const mockTransactions: Record<string, Transaction[]> = {
    'ACC001': [
      {
        date: '2024-09-20',
        type: 'BUY',
        units: 500.00,
        price: 100.00,
        amount: -50000.00,
        description: 'Purchase Growth Equity Fund'
      },
      {
        date: '2024-09-15',
        type: 'DIV',
        units: 0,
        price: 0,
        amount: 1250.00,
        description: 'Dividend Payment'
      },
      {
        date: '2024-09-10',
        type: 'BUY',
        units: 750.50,
        price: 100.00,
        amount: -75050.00,
        description: 'Additional Purchase'
      },
      {
        date: '2024-09-05',
        type: 'SELL',
        units: -200.00,
        price: 105.00,
        amount: 21000.00,
        description: 'Partial Sale'
      },
      {
        date: '2024-09-01',
        type: 'FEE',
        units: 0,
        price: 0,
        amount: -25.00,
        description: 'Management Fee'
      }
    ],
    'ACC002': [
      {
        date: '2024-09-18',
        type: 'BUY',
        units: 1000.00,
        price: 100.00,
        amount: -100000.00,
        description: 'Purchase Conservative Bond Fund'
      },
      {
        date: '2024-09-12',
        type: 'DIV',
        units: 0,
        price: 0,
        amount: 2800.00,
        description: 'Quarterly Dividend'
      },
      {
        date: '2024-09-08',
        type: 'BUY',
        units: 1000.00,
        price: 100.00,
        amount: -100000.00,
        description: 'Initial Purchase'
      }
    ]
  };

  const handleSearch = async () => {
    if (!accountId.trim()) {
      setError('Please enter an account ID');
      return;
    }

    setLoading(true);
    setError(null);
    setCurrentPage(1);

    setTimeout(() => {
      const foundTransactions = mockTransactions[accountId.toUpperCase()];
      if (foundTransactions) {
        setTransactions(foundTransactions);
      } else {
        setError('Account not found. Please check the account ID and try again.');
        setTransactions([]);
      }
      setLoading(false);
    }, 1000);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(Math.abs(amount));
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const getTransactionTypeColor = (type: string) => {
    switch (type) {
      case 'BUY': return 'primary';
      case 'SELL': return 'secondary';
      case 'DIV': return 'success';
      case 'FEE': return 'warning';
      default: return 'default';
    }
  };

  const getTransactionIcon = (type: string) => {
    switch (type) {
      case 'BUY': return <TrendingUp />;
      case 'SELL': return <TrendingDown />;
      default: return null;
    }
  };

  const paginatedTransactions = transactions.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const totalPages = Math.ceil(transactions.length / itemsPerPage);

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto', mt: 2 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <Button
            startIcon={<ArrowBack />}
            onClick={() => navigate('/')}
            sx={{ mr: 2 }}
          >
            Back to Menu
          </Button>
          <History sx={{ mr: 2, color: 'primary.main' }} />
          <Typography variant="h5" component="h1">
            Transaction History
          </Typography>
        </Box>

        <Box sx={{ mb: 4 }}>
          <Grid container spacing={2} alignItems="center">
            <Grid size={{ xs: 12, sm: 8 }}>
              <TextField
                fullWidth
                label="Account ID"
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                placeholder="Enter account ID (e.g., ACC001, ACC002)"
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <Button
                fullWidth
                variant="contained"
                startIcon={<Search />}
                onClick={handleSearch}
                disabled={loading}
                sx={{ height: 56 }}
              >
                Search
              </Button>
            </Grid>
          </Grid>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {loading && <LoadingSpinner message="Retrieving transaction history..." />}

        {transactions.length > 0 && !loading && (
          <>
            <Box sx={{ mb: 2 }}>
              <Typography variant="h6" gutterBottom>
                Account: {accountId.toUpperCase()}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Showing {transactions.length} transaction(s)
              </Typography>
            </Box>

            <TableContainer component={Paper} elevation={1}>
              <Table>
                <TableHead>
                  <TableRow sx={{ backgroundColor: 'grey.50' }}>
                    <TableCell><strong>Date</strong></TableCell>
                    <TableCell><strong>Type</strong></TableCell>
                    <TableCell align="right"><strong>Units</strong></TableCell>
                    <TableCell align="right"><strong>Price</strong></TableCell>
                    <TableCell align="right"><strong>Amount</strong></TableCell>
                    <TableCell><strong>Description</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {paginatedTransactions.map((transaction, index) => (
                    <TableRow 
                      key={index}
                      sx={{ '&:nth-of-type(odd)': { backgroundColor: 'action.hover' } }}
                    >
                      <TableCell>{formatDate(transaction.date)}</TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          {getTransactionIcon(transaction.type)}
                          <Chip
                            label={transaction.type}
                            color={getTransactionTypeColor(transaction.type) as any}
                            size="small"
                          />
                        </Box>
                      </TableCell>
                      <TableCell align="right">
                        {transaction.units !== 0 ? transaction.units.toFixed(2) : '-'}
                      </TableCell>
                      <TableCell align="right">
                        {transaction.price !== 0 ? formatCurrency(transaction.price) : '-'}
                      </TableCell>
                      <TableCell 
                        align="right"
                        sx={{ 
                          color: transaction.amount >= 0 ? 'success.main' : 'error.main',
                          fontWeight: 'medium'
                        }}
                      >
                        {transaction.amount >= 0 ? '+' : '-'}{formatCurrency(transaction.amount)}
                      </TableCell>
                      <TableCell>{transaction.description}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            {totalPages > 1 && (
              <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
                <Pagination
                  count={totalPages}
                  page={currentPage}
                  onChange={(_, page) => setCurrentPage(page)}
                  color="primary"
                />
              </Box>
            )}

            <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 3 }}>
              Data retrieved from DB2 transaction history tables
            </Typography>
          </>
        )}

        {transactions.length === 0 && !loading && !error && (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="body1" color="text.secondary">
              Enter an account ID to view transaction history
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Try: ACC001 or ACC002 for sample data
            </Typography>
          </Box>
        )}
      </Paper>
    </Box>
  );
};

export default TransactionHistory;
