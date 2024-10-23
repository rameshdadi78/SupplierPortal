import React, { useEffect, useState } from "react";
import API_BASE_URL from '../config';
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import "../Styles/login.css";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import Cookies from "js-cookie";
import { jwtDecode } from 'jwt-decode'; // For decoding JWT and checking its validity

const setJwtCookie = (token) => {
	//sessionStorage.setItem('jwtToken', token);
  Cookies.set("jwtToken", token, { expires: 7 }); // Expires in 7 days
};

const isTokenValid = (token) => {
  if (!token) return false;

  try {
    // Decode the token
    const decoded = jwtDecode(token);
	
    // Extract the expiration time
	
	// Extract the expiration date string
    const expirationDateStr = decoded.DateAndTime; // Adjust based on your token structure

    // Convert the expiration date string to a Date object
    const expDate = new Date(expirationDateStr);
	
   //alert(expDate.getTime());
    // Check if the token has expired
	//needs to be uncommented later
   // return Date.now() <  expDate.getTime();;
  
   return true;
  } catch (error) {
    console.error("Error decoding token:", error);
    return false;
  }
};

const validateTokenWithServer = async (token) => {
  try {
	  //alert("Am i called????");
    const response = await fetch("/webapi/myresource/validateJWT", { // Replace with your API endpoint
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ token }),
    });

    if (!response.ok) {
      throw new Error("Network response was not ok");
    }
	debugger;
    const data = await response.json();
    return data.isValid; // Assuming server returns an object with `isValid` property
  } catch (error) {
    console.error("Token validation failed", error);
    return false;
  }
};

const Login = () => {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
    rememberMe: false,
  });
  const [errors, setErrors] = useState({});
  const [alertDisplayed, setAlertDisplayed] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
	 const token = Cookies.get("jwtToken");
	//alert(token);
    if (token && isTokenValid(token)) {
		//alert("Yes token is valid");
      validateTokenWithServer(token).then(isValid => {
		 
        if (isValid) {
          navigate("/home");
        } else {
          // Optionally handle invalid tokens
          //Cookies.remove("jwtToken");
        }
      });
    }else{
		// alert("Token is in valid");
	}
  }, [navigate]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const validateForm = () => {
    let newErrors = {};
    if (!formData.username.trim()) {
      newErrors.username = "Username is required";
    }
    if (!formData.password) {
      newErrors.password = "Password is required";
    } else if (formData.password.length < 4) {
      newErrors.password = "Password must be at least 4 characters long";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
	
    if (validateForm()) {
      try {
        const response = await fetch(
          `${API_BASE_URL}/myresource/login`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
            },
            body: JSON.stringify({
              username: formData.username,
              password: formData.password,
            }),
          }
        );

        if (!response.ok) {
          throw new Error("Network response was not ok");
        }

        const data = await response.json();

        if (data.status === "successful") {
          localStorage.setItem('userData', JSON.stringify(data));
          if (!alertDisplayed) {
            toast.success("Logged in successfully");
            setAlertDisplayed(true);
          }

          setJwtCookie(data.jwt); // Store the JWT token in a cookie

          // Store the username in local storage
          localStorage.setItem("username", formData.username);
          localStorage.setItem("is_Active","false");

          // Redirect after a 2-second delay
          setTimeout(() => {
            navigate("/home");
          }, 2000);
        } else {
          toast.error("Login failed");
        }
      } catch (error) {
        console.error("There was an error logging in!", error);
        toast.error("Login failed due to error");
      }
    }
  };

  const handleForgotPassword = () => {
    navigate("/forgot-password");
  };

  return (
    <div
      className="login-container"
      // style={{ backgroundImage: `url("./assets/background1.jpg")` }}
    >
      <img className="background_login" src="./assets/background4.jpg" alt="" />
      <div className="login-overlay">
        <form className="login-form" onSubmit={handleSubmit}>
          <h2>Login</h2>
          <div className="form-group">
             <input
              type="text"
              name="username"
              placeholder="Username"
              value={formData.username}
              onChange={handleChange}
              className={errors.username ? "error" : ""}
            />
            {errors.username && (
              <span className="error-message">{errors.username}</span>
            )}
          </div>
          <div className="form-group password-wrapper">
            <input
              type={showPassword ? "text" : "password"}
              name="password"
              placeholder="Password"
              value={formData.password}
              onChange={handleChange}
              className={errors.password ? "error" : ""}
            />
            <div
              className={`password-toggle-icon ${
                errors.password ? "error-icon" : ""
              }`}
              onClick={() => setShowPassword((prev) => !prev)}
            >
              {showPassword ? <FaEye /> : <FaEyeSlash />}
            </div>
            {errors.password && (
              <span className="error-message">{errors.password}</span>
            )}
          </div>
          <div className="form-options">
            <label>
              <input
                type="checkbox"
                name="rememberMe"
                checked={formData.rememberMe}
                onChange={handleChange}
              />
              Remember me
            </label>
            <a href="#forgot-password" onClick={handleForgotPassword}>
              Forgot password?
            </a>
          </div>
          <button type="submit">Login</button>
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

export default Login;
