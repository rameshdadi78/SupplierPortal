import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import "../Styles/ResetPassword.css"

const ResetPassword = () => {
  const [formData, setFormData] = useState({
    username: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [errors, setErrors] = useState({});
  const [alertDisplayed, setAlertDisplayed] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const storedUsername = localStorage.getItem("username");
    if (storedUsername) {
      setFormData((prevData) => ({
        ...prevData,
        username: storedUsername,
      }));
    }
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const validateForm = () => {
    let newErrors = {};
    if (!formData.newPassword) {
      newErrors.newPassword = "New Password is required";
    } else if (formData.newPassword.length < 4) {
      newErrors.newPassword = "New Password must be at least 4 characters long";
    }
    if (formData.newPassword !== formData.confirmPassword) {
      newErrors.confirmPassword = "Passwords do not match";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (validateForm()) {
      try {
        const response = await fetch(
          "http://localhost:8081/Supplierportal/webapi/myresource/update",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
            },
            body: JSON.stringify({
              username: formData.username,
              newPassword: formData.newPassword,
            }),
          }
        );

        if (!response.ok) {
          throw new Error("Network response was not ok");
        }

        const data = await response.json();

        if (data.status === "successful") {
          if (!alertDisplayed) {
            toast.success("Password reset successfully");
            setAlertDisplayed(true);
            setTimeout(() => {
              navigate("/");
            }, 2000);
          }
        } else {
          toast.error("Failed to reset password");
        }
      } catch (error) {
        console.error("There was an error resetting the password!", error);
        toast.error("Reset password failed due to error");
      }
    }
  };

  return (
    <div
      className="reset-password-container"
      style={{ backgroundImage: `url("./assets/background1.jpg")` }}
    >
      <div className="reset-password-overlay">
        <form className="reset-password-form" onSubmit={handleSubmit}>
          <h2>Reset Password</h2>
          <div className="form-group">
            <h3>{formData.username}</h3>
          </div>
          <div className="form-group">
            <input
              type="password"
              name="newPassword"
              placeholder="New Password"
              value={formData.newPassword}
              onChange={handleChange}
              className={errors.newPassword ? "error" : ""}
            />
            {errors.newPassword && (
              <span className="error-message">{errors.newPassword}</span>
            )}
          </div>
          <div className="form-group">
            <input
              type="password"
              name="confirmPassword"
              placeholder="Confirm Password"
              value={formData.confirmPassword}
              onChange={handleChange}
              className={errors.confirmPassword ? "error" : ""}
            />
            {errors.confirmPassword && (
              <span className="error-message">{errors.confirmPassword}</span>
            )}
          </div>
          <button type="submit">Reset Password</button>
        </form>
      </div>
      <ToastContainer
        position="top-right"
        autoClose={5000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
      />
    </div>
  );
};

export default ResetPassword;
