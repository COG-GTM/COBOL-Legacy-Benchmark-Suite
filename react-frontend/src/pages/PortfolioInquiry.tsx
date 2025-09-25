import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  Card,
  CardContent,
  Divider,
  Alert,
  Chip
} from '@mui/material';
import {
  Search,
  ArrowBack,
  AccountBalance,
  TrendingUp,
  AttachMoney
} from '@mui/icons-material';
import LoadingSpinner from '../components/LoadingSpinner';

interface PortfolioPosition {
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
  gainLoss: number;
  gainLossPercent: number;
}

const PortfolioInquiry: React.FC = () => {
  const navigate = useNavigate();
  const [accountId, setAccountId] = useState('');
  const [loading, setLoading] = useState(false);
  const [position, setPosition] = useState<PortfolioPosition | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mockPositions: Record<string, PortfolioPosition> = {
    'ACC001': {
      fundId: 'FUND01',
      fundName: 'Growth Equity Fund',
      units: 1250.50,
      costBasis: 125000.00,
      marketValue: 142750.00,
      gainLoss: 17750.00,
      gainLossPercent: 14.2
    },
    'ACC002': {
      fundId: 'FUND02',
      fundName: 'Conservative Bond Fund',
      units: 2000.00,
      costBasis: 200000.00,
      marketValue: 205600.00,
      gainLoss: 5600.00,
      gainLossPercent: 2.8
    }
  };

  const handleSearch = async () => {
    if (!accountId.trim()) {
      setError('Please enter an account ID');
      return;
    }

    setLoading(true);
    setError(null);

    setTimeout(() => {
      const foundPosition = mockPositions[accountId.toUpperCase()];
      if (foundPosition) {
        setPosition(foundPosition);
      } else {
        setError('Account not found. Please check the account ID and try again.');
        setPosition(null);
      }
      setLoading(false);
    }, 1000);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const formatNumber = (num: number, decimals: number = 2) => {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    }).format(num);
  };

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto', mt: 2 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <Button
            startIcon={<ArrowBack />}
            onClick={() => navigate('/')}
            sx={{ mr: 2 }}
          >
            Back to Menu
          </Button>
          <Typography variant="h5" component="h1">
            Portfolio Position Inquiry
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

        {loading && <LoadingSpinner message="Retrieving portfolio position..." />}

        {position && !loading && (
          <Card elevation={2}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <AccountBalance sx={{ mr: 2, color: 'primary.main' }} />
                <Typography variant="h6">
                  Account: {accountId.toUpperCase()}
                </Typography>
              </Box>

              <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 6 }}>
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Fund ID
                    </Typography>
                    <Typography variant="h6" color="primary">
                      {position.fundId}
                    </Typography>
                  </Box>
                  
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Fund Name
                    </Typography>
                    <Typography variant="body1">
                      {position.fundName}
                    </Typography>
                  </Box>
                  
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Units
                    </Typography>
                    <Typography variant="h6">
                      {formatNumber(position.units)}
                    </Typography>
                  </Box>
                </Grid>

                <Grid size={{ xs: 12, md: 6 }}>
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Cost Basis
                    </Typography>
                    <Typography variant="h6">
                      {formatCurrency(position.costBasis)}
                    </Typography>
                  </Box>
                  
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Market Value
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <AttachMoney sx={{ mr: 1, color: 'success.main' }} />
                      <Typography variant="h6" color="success.main">
                        {formatCurrency(position.marketValue)}
                      </Typography>
                    </Box>
                  </Box>
                  
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Gain/Loss
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <TrendingUp 
                        sx={{ 
                          color: position.gainLoss >= 0 ? 'success.main' : 'error.main' 
                        }} 
                      />
                      <Typography 
                        variant="h6" 
                        color={position.gainLoss >= 0 ? 'success.main' : 'error.main'}
                      >
                        {formatCurrency(position.gainLoss)}
                      </Typography>
                      <Chip
                        label={`${position.gainLossPercent >= 0 ? '+' : ''}${position.gainLossPercent}%`}
                        color={position.gainLoss >= 0 ? 'success' : 'error'}
                        size="small"
                      />
                    </Box>
                  </Box>
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />
              
              <Typography variant="body2" color="text.secondary" align="center">
                Data retrieved from VSAM portfolio files
              </Typography>
            </CardContent>
          </Card>
        )}

        {!position && !loading && !error && (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="body1" color="text.secondary">
              Enter an account ID to view portfolio position details
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

export default PortfolioInquiry;
