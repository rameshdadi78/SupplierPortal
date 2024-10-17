import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../Styles/ForgotPassword.css"


const ForgotPassword = () => {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [errors, setErrors] = useState({});
  const navigate = useNavigate();

  const handleChange = (e) => {
    setEmail(e.target.value);
  };

  const validateForm = () => {
    let newErrors = {};
    if (!email.trim()) {
      newErrors.email = "Email is required";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (validateForm()) {
      const isSuccess = true;

      if (isSuccess) {
        setMessage("Password reset link has been sent to your email.");
        setMessageType("success");

        setTimeout(() => {
          navigate("/reset-password");
        }, 2000);
      } else {
        setMessage("Failed to send reset link.");
        setMessageType("error");
      }
    }
  };

  return (
    <div
      className="forgot-password-container"
      style={{ backgroundImage: `url("./assets/background1.jpg")` }}
    >
      <div className="forgot-password-overlay">
        <form className="forgot-password-form" onSubmit={handleSubmit}>
          <h2>Forgot Password</h2>
          {message && <p className={`message ${messageType}`}>{message}</p>}
          <div className="form-group">
            <input
              type="email"
              name="email"
              placeholder="Enter your email"
              value={email}
              onChange={handleChange}
              className={errors.email ? "error" : ""}
            />
            {errors.email && (
              <span className="error-message">{errors.email}</span>
            )}
          </div>
          <button type="submit">Send Reset Link</button>
        </form>
      </div>
    </div>
  );
};

export default ForgotPassword;
