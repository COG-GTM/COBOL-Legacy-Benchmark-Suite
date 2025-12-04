import { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Button,
  Paper,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Stack,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { TransactionHistory as TransactionHistoryType } from '../../types';

function TransactionHistory() {
  const [accountId, setAccountId] = useState('');
  const [history, setHistory] = useState<TransactionHistoryType | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = () => {
    setError(null);
    setHistory(null);
    if (!accountId.trim()) {
      setError('Please enter an account ID');
      return;
    }
    setHistory(null);
  };

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto', mt: 4 }}>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="h5" gutterBottom>
          Transaction History Inquiry
        </Typography>

        <Box sx={{ display: 'flex', gap: 2, mb: 3, alignItems: 'center' }}>
          <TextField
            id="accountId"
            label="Account"
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
            placeholder="Enter Account ID"
            inputProps={{ maxLength: 10 }}
            size="small"
            sx={{ minWidth: 200 }}
          />
          <Button variant="contained" onClick={handleSearch} startIcon={<SearchIcon />}>
            Search
          </Button>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {history && history.transactions.length > 0 && (
          <TableContainer component={Paper} variant="outlined" sx={{ mb: 3 }}>
            <Table>
              <TableHead>
                <TableRow sx={{ backgroundColor: 'primary.main' }}>
                  <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Date</TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Type</TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 'bold' }} align="right">
                    Units
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 'bold' }} align="right">
                    Price
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 'bold' }} align="right">
                    Amount
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {history.transactions.map((txn, index) => (
                  <TableRow key={index} hover>
                    <TableCell>{txn.date}</TableCell>
                    <TableCell>{txn.type}</TableCell>
                    <TableCell align="right">{txn.units.toLocaleString()}</TableCell>
                    <TableCell align="right">${txn.price.toFixed(2)}</TableCell>
                    <TableCell align="right">${txn.amount.toFixed(2)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}

        <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
          <Chip label="PF3=Exit" variant="outlined" size="small" />
          <Chip label="PF7=Previous" variant="outlined" size="small" />
          <Chip label="PF8=Next" variant="outlined" size="small" />
        </Stack>
      </Paper>
    </Box>
  );
}

export default TransactionHistory;
