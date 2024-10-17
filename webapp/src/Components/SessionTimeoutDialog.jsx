import * as React from 'react';
import { useContext } from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Paper } from '@mui/material';
import { useNavigate } from "react-router-dom";
import Draggable from 'react-draggable';
import { FirstContext } from "../App";
import WarningAmberIcon from '@mui/icons-material/WarningAmber';


export default function SessionTimeoutDialog() {

  const {sessionTimeoutOpen,setSessionTimeoutOpen} = useContext(FirstContext);

  const navigate = useNavigate();

  const handleClose = (event, reason) => {
    if (reason === 'backdropClick' || reason === 'escapeKeyDown') {
      return;
    }
    setSessionTimeoutOpen(false);
  };
  const handleLoginAgain = () => {
    setSessionTimeoutOpen(false);
    navigate("/login");
  };

  return (
    <Dialog
      open={sessionTimeoutOpen}
      onClose={handleClose}
      aria-labelledby="draggable-dialog-title"
    >
      {/* <DialogTitle id="draggable-dialog-title" sx={{color:'var(--blue)', display:'flex', justifyContent:'flex-start',alignItems:'center'}}>
          <WarningAmberIcon/> 
          Session Timeout
      </DialogTitle> */}
      <DialogContent   sx={{marginTop:'15px' ,paddingBottom:'10px'}}>
        <DialogContentText sx={{color:'var(--black)', display:'flex', justifyContent:'flex-start',alignItems:'space-evenly',gap:'15px'}}>
          <WarningAmberIcon sx={{color:'var(--dark_red)', fontSize:'45px'}}/> 
          Your session has expired due to inactivity. Please log in again to continue using the website.
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleLoginAgain} sx={{fontWeight:'550',border:'1.5px solid var(--blue)'}}>OK</Button>
      </DialogActions>
    </Dialog>
  );
}
