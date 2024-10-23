import * as React from 'react';
import API_BASE_URL from '../config';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';
import { styled } from '@mui/material/styles';

export const DownloadSnackbar = ({ open, message, handleClose }) => {
  // Customizing the Alert using the sx prop
  return (
    <Snackbar
      open={open}
      autoHideDuration={2000}
      onClose={handleClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }} 
    >
      <Alert
        onClose={handleClose}
        severity="success"
        variant="filled"
        sx={{
          backgroundColor: '#fff', 
          color:'var(--black)', 
          fontSize: '13.5px', 
          border: '1.5px solid var(--blue)', 
          boxShadow: '0 4px 10px rgba(0, 0, 0, 0.2)', 
        }}
      >
        {message}
      </Alert>
    </Snackbar>
  );
};
