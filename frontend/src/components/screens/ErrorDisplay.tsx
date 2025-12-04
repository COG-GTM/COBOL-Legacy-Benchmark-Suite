import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Button,
  Table,
  TableBody,
  TableCell,
  TableRow,
} from '@mui/material';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import { ErrorInfo } from '../../types';

interface ErrorDisplayProps {
  error?: ErrorInfo;
}

function ErrorDisplay({ error }: ErrorDisplayProps) {
  const navigate = useNavigate();

  const defaultError: ErrorInfo = {
    code: '404',
    message: 'Page Not Found',
    details: 'The requested page could not be found.',
  };

  const displayError = error || defaultError;

  const handleContinue = () => {
    navigate('/menu');
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', mt: 4 }}>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          <ErrorOutlineIcon color="error" fontSize="large" />
          <Typography variant="h5" color="error">
            System Error
          </Typography>
        </Box>

        <Table>
          <TableBody>
            <TableRow>
              <TableCell component="th" sx={{ fontWeight: 'bold', width: '30%' }}>
                Error Code
              </TableCell>
              <TableCell sx={{ color: 'error.main' }}>{displayError.code}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell component="th" sx={{ fontWeight: 'bold' }}>
                Details
              </TableCell>
              <TableCell sx={{ color: 'error.main' }}>{displayError.message}</TableCell>
            </TableRow>
            {displayError.details && (
              <TableRow>
                <TableCell colSpan={2} sx={{ color: 'text.secondary' }}>
                  {displayError.details}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>

        <Box sx={{ mt: 3, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Press ENTER to continue
          </Typography>
          <Button variant="contained" onClick={handleContinue}>
            Continue
          </Button>
        </Box>
      </Paper>
    </Box>
  );
}

export default ErrorDisplay;
