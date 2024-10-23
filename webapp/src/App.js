import { createContext, useState, useEffect } from "react";
import API_BASE_URL from "./config";
import {
  BrowserRouter as Router,
  Route,
  Routes,
  Navigate,
} from "react-router-dom";
import "./App.css";
import Nav from "./Components/Nav";
import { useTranslation } from "react-i18next";
import Login from "./Components/login.jsx";
import ForgotPassword from "./Components/ForgotPassword";
import ResetPassword from "./Components/ResetPassword";
import MainSection from "./Components/MainSection.jsx";
import SessionTimeoutDialog from "./Components/SessionTimeoutDialog.jsx";
import { Changes_Page } from "./Components/Changes_Page.jsx";
import { Devaition_Page } from "./Components/Devaition_Page.jsx";

// Context to be used across the app
export const FirstContext = createContext();

export const HomePage = () => {
  const { i18n, t } = useTranslation();

  // Language data for language switcher
  const languagesData = [
    { code: "en", lang: "English" },
    { code: "fr", lang: "French" },
    { code: "hi", lang: "Hindi" },
  ];

  const changingLanguage = (lng) => {
    i18n.changeLanguage(lng);
    setSelectedLang(languagesData.find((lang) => lang.code === lng).lang);
  };

  // State to manage which component is visible
  const [assignedParts, setAssignedParts] = useState(true);
  const [changesOpen, setChangesOpen] = useState(false);
  const [deviationOpen, setDeviationOpen] = useState(false);
  const [selectedTheme, setSelectedTheme] = useState("Default");
  const [sessionTimeoutOpen, setSessionTimeoutOpen] = useState(false);
  const [selectedLang, setSelectedLang] = useState("English");
  const [selectedRow, setSelectedRow] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [searchList, showSearchList] = useState(false);
  const [selectedOption, setSelectedOption] = useState("Everything");
  const [supplierName, setSupplierName] = useState("");
  const [objectIds, setObjectIds] = useState([]);
  const [caObjectIds, setCaObjectIds] = useState([]);
  const [devObjectIds, setdevObjectIds] = useState([]);
  const [visiblePartIds, setVisiblePartIds] = useState([]);
  const [visibleSpec, setvisibleSpec] = useState([]);
  const [isVisible, setisVisible] = useState([]);
  const [visibleItem, setVisibleItem] = useState([]);
  const [type, setType] = useState([]);
  const [activeTab, setActiveTab] = useState("assignedParts");
  const [partsCount, setPartCount] = useState(0);
  const [caCount, setCaCount] = useState(0);
  const [devCount, setDevCount] = useState(0);
  // Track visibility of each tab based on fetched data
  const [hasPartsData, setHasPartsData] = useState(true);
  const [hasChangesData, setHasChangesData] = useState(true);
  const [hasDeviationData, setHasDeviationData] = useState(true);

  const handleChangePage = () => {
    setAssignedParts(false);
    setChangesOpen(true);
    setDeviationOpen(false);
  };

  const handleAssignPart = () => {
    setAssignedParts(true);
    setChangesOpen(false);
    setDeviationOpen(false);
  };

  const handleDeviationPage = () => {
    setAssignedParts(false);
    setChangesOpen(false);
    setDeviationOpen(true);
  };

  const handleSearchChange = (term) => {
    setSearchTerm(term);
    if (term.length === 0) {
      setSearchTerm("");
      setVisiblePartIds([]);
      setCaObjectIds([]);
      setdevObjectIds([]);
      setHasPartsData(true);
      setHasChangesData(true);
      setHasDeviationData(true);
    }
  };

  useEffect(() => {
    const fetchSearchdata = async () => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/SupplierPortal/searchAll`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              text: searchTerm,
              field: selectedOption,
              suppliername: supplierName,
            }),
          }
        );

        if (response.ok) {
          const data = await response.json();

          const newVisiblePartIds = [];
          const newVisibleDeviation = [];
          const newVisibleCA = [];
          const newVisibleItems = [];
          const newVisibleSpecs = [];
          const newisVisible = [];
          data.results.forEach((result) => {
            if (result.parts) {
              result.parts.forEach((part) => {
                const [partId, itemVisible, specVisible, isVisible] =
                  part.split("|");
                newVisiblePartIds.push(partId);
                newVisibleItems.push({ partId, itemVisible }); // Store as is (string)
                newVisibleSpecs.push({ partId, specVisible }); // Store as is (string)
                newisVisible.push({ partId, isVisible });
              });
            }
            if (result.deviation) {
              newVisibleDeviation.push(...result.deviation);
            }
            if (result.ca) {
              newVisibleCA.push(...result.ca);
            }
          });
          setPartCount(newVisiblePartIds.length);
          setCaCount(newVisibleCA.length);
          setDevCount(newVisibleDeviation.length);
          setVisiblePartIds(newVisiblePartIds);
          setdevObjectIds(newVisibleDeviation);
          setCaObjectIds(newVisibleCA);
          setVisibleItem(newVisibleItems);
          setvisibleSpec(newVisibleSpecs);
          setisVisible(newisVisible);
          // Update visibility state based on fetched data

          setHasPartsData(newVisiblePartIds.length > 0);
          setHasChangesData(newVisibleCA.length > 0);
          setHasDeviationData(newVisibleDeviation.length > 0);

          // Set default active tab based on available data
          if (newVisiblePartIds.length > 0) setActiveTab("assignedParts");
          else if (newVisibleCA.length > 0) setActiveTab("changesOpen");
          else if (newVisibleDeviation.length > 0)
            setActiveTab("deviationOpen");
        } else {
          console.error("Error fetching data:", response.status);
        }
      } catch (error) {
        console.error("Error:", error);
      }
    };

    // Trigger the fetch only if searchTerm and selectedOption are valid
    if (searchTerm && selectedOption && searchTerm.length > 3) {
      fetchSearchdata();
    }
  }, [searchTerm, selectedOption, supplierName]);

  return (
    <div>
      <FirstContext.Provider
        value={{
          selectedTheme,
          setSelectedTheme,
          selectedLang,
          changingLanguage,
          languagesData,
          t,
          sessionTimeoutOpen,
          setSessionTimeoutOpen,
          handleChangePage,
          handleAssignPart,
          handleDeviationPage,
          activeTab,
          setActiveTab,
        }}
      >
        <Nav
          searchTerm={searchTerm}
          handleSearchChange={handleSearchChange}
          searchList={searchList}
          selectedOption={selectedOption}
          handleOptionClick={setSelectedOption}
          handleSearchList={() => showSearchList((prev) => !prev)}
          partsCount={partsCount}
          caCount={caCount}
          newDevCount={devCount}
        />

        {/* Conditionally render sections based on available data */}
        {hasPartsData && assignedParts && (
          <MainSection
            selectedRow={selectedRow}
            setSelectedRow={setSelectedRow}
            setSupplierName={setSupplierName}
            visiblePartIds={visiblePartIds}
            visibleSpec={visibleSpec}
            isVisible={isVisible}
            visibleItem={visibleItem}
          />
        )}
        {hasChangesData && changesOpen && (
          <Changes_Page
            selectedRData={selectedRow}
            setSelectedRData={setSelectedRow}
            caObjectIds={caObjectIds}
          />
        )}
        {hasDeviationData && deviationOpen && (
          <Devaition_Page devObjectIds={devObjectIds} />
        )}

        <SessionTimeoutDialog />
      </FirstContext.Provider>
    </div>
  );
};

// PrivateRoute component to handle protected routes
const PrivateRoute = ({ element }) => {
  const jwt =
    (JSON.parse(localStorage.getItem("userData")) &&
      JSON.parse(localStorage.getItem("userData")).jwt) ||
    false;

  const isAuthenticated = Boolean(jwt);
  return isAuthenticated ? element : <Navigate to="/login" />;
};

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        {/* Protected Route */}
        <Route path="/home" element={<PrivateRoute element={<HomePage />} />} />

        {/* Catch-all Route for undefined paths, redirect to home if authenticated */}
        <Route path="*" element={<PrivateRoute element={<HomePage />} />} />
      </Routes>
    </Router>
  );
}

export default App;
