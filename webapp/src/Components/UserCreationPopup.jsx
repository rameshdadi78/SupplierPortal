import React, { useState } from "react";
import API_BASE_URL from "../config";
// import './UserCreation.css';  // CSS file for styling
import "../Styles/UserCreation.css";
import Swal from "sweetalert2"; // Import SweetAlert2

export const UserCreationPopup = ({ onClose }) => {
  const [userType, setUserType] = useState("SingleUser"); // Toggle between Single User and Multiple User
  const [fileName, setFileName] = useState(""); // To hold the file name for Multiple User
  const [fileData, setFileData] = useState(null); // To hold the actual file content for Multiple User
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    confirmPassword: "",
    username: "",
    description: "",
    country: "",
  });
  const [errors, setErrors] = useState({}); // To hold validation errors

  // Handle radio button change
  const handleUserTypeChange = (event) => {
    setUserType(event.target.value);
  };

  // Handle form input changes for single user
  const handleInputChange = (event) => {
    const { name, value } = event.target;
    setFormData({ ...formData, [name]: value });
    setErrors({ ...errors, [name]: "" }); // Clear error when typing
    console.log(formData.email);
  };

  // Validate form on submit
  const validateForm = () => {
    const newErrors = {};

    if (!formData.firstName) newErrors.firstName = "First Name is required";
    if (!formData.lastName) newErrors.lastName = "Last Name is required";
    if (!formData.username) newErrors.username = "User Name is required";
    if (!formData.email) newErrors.email = "Email is required";
    if (!formData.country) newErrors.country = "Country is required";
    if (!formData.password) newErrors.password = "Password is required";
    if (!formData.confirmPassword)
      newErrors.confirmPassword = "Confirm Password is required";
    if (formData.password !== formData.confirmPassword)
      newErrors.confirmPassword = "Passwords do not match";

    return newErrors;
  };
  const handleSubmit = async (event) => {
    event.preventDefault(); // Ensure default form submission is prevented

    console.log("Form is being submitted"); // This should appear when "Create" is clicked

    // Validate form input
    const validationErrors = validateForm();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors); // Set validation errors if present
      console.log("Validation failed", validationErrors);
      return; // Stop if validation fails
    }

    // Create the payload for the web service
    const payload = {
      email: formData.email,
      username: formData.username,
      firstname: formData.firstName,
      lastname: formData.lastName,
      password: formData.password,
      confirmpassword: formData.confirmPassword,
      country: formData.country, // country
    };

    console.log("Payload being sent:", payload); // Ensure payload is correct

    try {
      // Make the API call
      const response = await fetch(`${API_BASE_URL}/myresource/signup`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const result = await response.json(); // Parse JSON response

      // Inside your handleSubmit function:
      if (response.ok) {
        console.log("API response:", result);

        // Use SweetAlert2 for a success alert
        Swal.fire({
          icon: "success",
          title: "Success",
          text: result.message || "User created successfully!",
          confirmButtonText: "OK",
        }).then(() => {
          onClose(); // Close the popup when the alert is confirmed
          window.location.reload(); // Refresh the page
        });
      } else {
        console.error("Failed to create user:", result.error);

        // Use SweetAlert2 for an error alert
        Swal.fire({
          icon: "error",
          title: "Error",
          text: `Failed to create user: ${result.error}`,
          confirmButtonText: "OK",
        });
      }
    } catch (error) {
      console.error("Error during API call:", error);
      alert("An error occurred while creating user.");
    }
  };

  // Handle file upload for multiple users
  // Handle file upload for multiple users
  const handleFileUpload = (event) => {
    const file = event.target.files[0];
    if (file) {
      setFileName(file.name);
      setFileData(file); // Store the file content
    } else {
      setFileName(""); // Clear the file name if no file is selected
      setFileData(null); // Clear the file data
    }
  };

  const handleSubmitForMultiUser = async (event) => {
    event.preventDefault(); // Prevent default form submission

    if (!fileData) {
      Swal.fire({
        icon: "error",
        title: "Error",
        text: "Please upload a file to create multiple users.",
        confirmButtonText: "OK",
      });
      return;
    }

    const formData = new FormData(); // Create FormData object to handle file uploads
    // const response = await fetch('http://localhost:8081/Supplierportal/webapi/myresource/file', {
    formData.append("file", fileData); // Append the selected file
    console.log("fileData");
    console.log(fileData);
    try {
      const response = await fetch(`${API_BASE_URL}/myresource/userCreation`, {
        method: "POST",
        body: formData, // Send the FormData with the file
        // headers: {
        //     // Do not set Content-Type; it is set automatically by the browser for multipart/form-data
        // },
      });
      console.log("000Response:", response);

      // Log the response status
      console.log("11111Response Status:", response.status);

      // Log the response headers
      console.log("222222222Response Headers:", [...response.headers]);
      const contentType = response.headers.get("content-type");
      // Check if the response is JSON before parsing
      if (contentType && contentType.includes("application/json")) {
        const result = await response.json(); // Parse JSON response
        console.log("3333333333------Parsed Response:", result);

        if (response.ok) {
          // Show success message using SweetAlert2
          Swal.fire({
            icon: "success",
            title: "Success",
            text:
              result.message ||
              "User creation process completed. Please click OK to download the report with success and failure details!",
            confirmButtonText: "OK",
          }).then(() => {
            // Convert the JSON result to CSV
            const jsonToCsv = (json) => {
              const { headers, data } = json;
              // Join headers with commas
              const csvHeaders = headers.join(",");
              // Map data rows and join each value with commas
              const csvRows = data.map((row) =>
                headers
                  .map((header) => (row[header] ? row[header] : ""))
                  .join(",")
              );
              // Combine headers and rows into a CSV string
              return [csvHeaders, ...csvRows].join("\n");
            };

            const csvContent = jsonToCsv(result);

            // Create a Blob from the CSV content
            const blob = new Blob([csvContent], {
              type: "text/csv;charset=utf-8;",
            });

            // Create a link to download the Blob
            const link = document.createElement("a");
            const url = URL.createObjectURL(blob);
            link.setAttribute("href", url);
            link.setAttribute("download", "user_creation_result.csv");

            // Append link to the body, trigger click and remove it
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            // Close the popup and reload the page
            onClose();
            window.location.reload(); // Refresh the page
          });
        } else {
          // Show error message using SweetAlert2
          Swal.fire({
            icon: "error",
            title: "Error",
            text: `Failed to create users: ${result.error || "Unknown error"}`,
            confirmButtonText: "OK",
          });
        }
      } else {
        // Log full response details for debugging
        const errorText = await response.text();
        console.error("Unexpected response format:", errorText);

        Swal.fire({
          icon: "error",
          title: "Error",
          text: `Unexpected response from the server: ${errorText}`,
          confirmButtonText: "OK",
        });
      }
    } catch (error) {
      console.error("Error during API call:", error);
      Swal.fire({
        icon: "error",
        title: "Error",
        text: "An error occurred while creating users. Please try again later.",
        confirmButtonText: "OK",
      });
    }
  };

  return (
    <div className="user-creation-popup">
      <div className="popup-header">
        <div className="top_head_user_heading">
          <i class="fa-solid fa-user"></i>
          <p>User Creation</p>
        </div>

        {/* <button className="close-btn" onClick={onClose}><i class="fa-solid fa-x"></i></button> */}
        <i onClick={onClose} class="fa-solid fa-x close-btn"></i>
      </div>

      <div className="radio-buttons">
        <label>
          <input
            type="radio"
            value="SingleUser"
            checked={userType === "SingleUser"}
            onChange={handleUserTypeChange}
          />
          Single User
        </label>
        <label>
          <input
            type="radio"
            value="MultipleUser"
            checked={userType === "MultipleUser"}
            onChange={handleUserTypeChange}
          />
          Multiple User
        </label>
      </div>
      <div className="popup-content">
        {userType === "SingleUser" && (
          // <form className="single-user-form" onSubmit={handleSubmit}>

          <form onSubmit={handleSubmit}>
            <div className="single-user-form">
              <div>
                <label htmlFor="firstName">
                  First Name <span className="required-field">*</span>
                </label>
                <input
                  type="text"
                  name="firstName"
                  placeholder="Enter your first name"
                  value={formData.firstName}
                  onChange={handleInputChange}
                />
                {errors.firstName && (
                  <p className="error">{errors.firstName}</p>
                )}
              </div>
              <div>
                <label htmlFor="lastName">
                  Last Name <span className="required-field">*</span>
                </label>
                <input
                  type="text"
                  name="lastName"
                  placeholder="Enter your last name"
                  value={formData.lastName}
                  onChange={handleInputChange}
                />
                {errors.lastName && <p className="error">{errors.lastName}</p>}
              </div>
              <div>
                <label htmlFor="username">
                  Username <span className="required-field">*</span>
                </label>
                <input
                  type="text"
                  name="username"
                  placeholder="Enter your username"
                  value={formData.username}
                  onChange={handleInputChange}
                />
                {errors.username && <p className="error">{errors.username}</p>}
              </div>
              <div>
                <label htmlFor="email">
                  Email <span className="required-field">*</span>
                </label>
                <input
                  type="email"
                  name="email"
                  placeholder="Enter your email"
                  value={formData.email}
                  onChange={handleInputChange}
                />
                {errors.email && <p className="error">{errors.email}</p>}
              </div>
              <div>
                <label htmlFor="password">
                  Password <span className="required-field">*</span>
                </label>
                <input
                  type="password"
                  name="password"
                  placeholder="Enter your password"
                  value={formData.password}
                  onChange={handleInputChange}
                />
                {errors.password && <p className="error">{errors.password}</p>}
              </div>
              <div>
                <label htmlFor="confirmPassword">
                  Confirm Password <span className="required-field">*</span>
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  placeholder="Confirm your password"
                  value={formData.confirmPassword}
                  onChange={handleInputChange}
                />
                {errors.confirmPassword && (
                  <p className="error">{errors.confirmPassword}</p>
                )}
              </div>

              <div>
                <label htmlFor="description">Description</label>
                <input
                  type="text"
                  name="description"
                  placeholder="Enter your description"
                  value={formData.description}
                  onChange={handleInputChange}
                />
              </div>
              <div>
                <label htmlFor="country">
                  Country <span className="required-field">*</span>
                </label>
                <input
                  type="text"
                  name="country"
                  placeholder="Enter your country"
                  value={formData.country}
                  onChange={handleInputChange}
                />
                {errors.country && <p className="error">{errors.country}</p>}
              </div>
            </div>

            <div className="popup-footer">
              <button className="create-btn" type="submit">
                Create
              </button>
              <button className="cancel-btn" onClick={onClose}>
                Cancel
              </button>
            </div>
          </form>

          // </div>
        )}

        {userType === "MultipleUser" && (
          <div className="multiple-user-form">
            <div className="file-upload">
              <input
                type="text"
                value={fileName}
                placeholder="File Name"
                readOnly
              />
              <input
                type="file"
                id="file-upload"
                style={{ display: "none" }}
                onChange={handleFileUpload}
              />
              <button
                className="upload-btn"
                onClick={() => document.getElementById("file-upload").click()}
                type="button"
              >
                Upload File
              </button>
              {fileName && (
                <span
                  className="remove-file"
                  onClick={() => {
                    setFileName("");
                    setFileData(null); // Clear file data
                  }}
                >
                  X
                </span>
              )}
            </div>

            {/* Download Sample File */}
            <a
              href="./SampleUserCreationFile.xls"
              download
              className="sample-file-download"
            >
              Download Sample File
            </a>

            <div className="popup-footer">
              <button
                className="create-btn"
                type="button"
                onClick={handleSubmitForMultiUser}
              >
                Create
              </button>
              <button className="cancel-btn" type="button" onClick={onClose}>
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
