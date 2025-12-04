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
  TableRow,
  Chip,
  Stack,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { PortfolioPosition } from '../../types';

function PortfolioInquiry() {
  const [accountId, setAccountId] = useState('');
  const [position, setPosition] = useState<PortfolioPosition | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = () => {
    setError(null);
    setPosition(null);
    if (!accountId.trim()) {
      setError('Please enter an account ID');
      return;
    }
    setPosition(null);
  };

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', mt: 4 }}>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="h5" gutterBottom>
          Portfolio Position Inquiry
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

        {position && (
          <TableContainer component={Paper} variant="outlined" sx={{ mb: 3 }}>
            <Table>
              <TableBody>
                <TableRow>
                  <TableCell component="th" sx={{ fontWeight: 'bold', width: '30%' }}>
                    Fund ID
                  </TableCell>
                  <TableCell>{position.fundId}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell component="th" sx={{ fontWeight: 'bold' }}>
                    Fund Name
                  </TableCell>
                  <TableCell>{position.fundName}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell component="th" sx={{ fontWeight: 'bold' }}>
                    Units
                  </TableCell>
                  <TableCell>{position.units.toLocaleString()}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell component="th" sx={{ fontWeight: 'bold' }}>
                    Cost Basis
                  </TableCell>
                  <TableCell>${position.costBasis.toFixed(2)}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell component="th" sx={{ fontWeight: 'bold' }}>
                    Market Value
                  </TableCell>
                  <TableCell>${position.marketValue.toFixed(2)}</TableCell>
                </TableRow>
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

export default PortfolioInquiry;
