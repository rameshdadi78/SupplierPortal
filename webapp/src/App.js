import { createContext, useState, useEffect } from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from "react-router-dom";
import './App.css';
import Nav from './Components/Nav';
import { useTranslation } from 'react-i18next';
import Login from "./Components/login.jsx";
import ForgotPassword from "./Components/ForgotPassword";
import ResetPassword from "./Components/ResetPassword";
import MainSection from './Components/MainSection.jsx';
import SessionTimeoutDialog from './Components/SessionTimeoutDialog.jsx';
import { Changes_Page } from './Components/Changes_Page.jsx';
import { Devaition_Page } from './Components/Devaition_Page.jsx';



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
    setSelectedLang(languagesData.find(lang => lang.code === lng).lang);
  };

  // State to manage which component is visible
  const [assignedParts, setAssignedParts] = useState(true);
  const [changesOpen, setChangesOpen] = useState(false);
  const [deviationOpen, setDeviationOpen] = useState(false);
  const [selectedTheme, setSelectedTheme] = useState("Default");
  const [sessionTimeoutOpen, setSessionTimeoutOpen] = useState(false);
  const [selectedLang, setSelectedLang] = useState('English');
  const [selectedRow, setSelectedRow] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [searchList, showSearchList] = useState(false);
  const [selectedOption, setSelectedOption] = useState('Everything');
  const [supplierName, setSupplierName] = useState('');
  const [objectIds, setObjectIds] = useState([]);
  const [caObjectIds, setCaObjectIds] = useState([]);
  const [devObjectIds, setdevObjectIds] = useState([]);
  const [visiblePartIds, setVisiblePartIds] = useState([]);
  const [visibleSpec, setvisibleSpec] = useState([]);
  const [visibleItem, setVisibleItem] = useState([]);
  const [type, setType] = useState([]);
  const [activeTab, setActiveTab] = useState('assignedParts');
  // Handlers to switch views
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
    setSearchTerm(term); // Update state with the term if length is sufficient
   if (term.length === 0) {
      setSearchTerm(''); // Set search term to empty if term is empty
      setVisiblePartIds([]); // Set visiblePartIds to empty if term is empty
      setCaObjectIds([]);
      setdevObjectIds([]);
  }

};


useEffect(() => {
  const fetchDataForCA = async () => {
    try {
      const response = await fetch("http://localhost:8081/Supplierportal/webapi/SupplierPortal/searchForCA", {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          text: searchTerm,
          field: selectedOption,
        }),
      });

      if (response.ok) {
        const data = await response.json();


        if (data.results && data.results.length > 0) {
          setAssignedParts(false);
          setChangesOpen(true);
          setDeviationOpen(false);
          setActiveTab('changes');
        }

        const results = data.results || [];
        const newCaIds = [];

        results.forEach(result => {
          const caKey = "type: Change Action";
          const caInfo = result[caKey];

          if (caInfo && caInfo.caid) {
            const caIds = caInfo.caid.includes("|")
              ? caInfo.caid.split("|")
              : [caInfo.caid];
            newCaIds.push(...caIds);
          }
        });
        
        setCaObjectIds(newCaIds);
      } else {
        console.error('Error fetching data:', response.status);
      }
    } catch (error) {
      console.error('Error:', error);
    }
  };

  if (searchTerm && selectedOption && searchTerm.length > 3) {
    fetchDataForCA();
  }
}, [searchTerm, selectedOption]);

useEffect(() => {
  const fetchDataForDeviation = async () => {
    try {
      const response = await fetch("http://localhost:8081/Supplierportal/webapi/SupplierPortal/searchForDeviation", {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          text: searchTerm,
          field: selectedOption,
        }),
      });

      if (response.ok) {
        const data = await response.json();


        if (data.results && data.results.length > 0) {
          setAssignedParts(false);
          setChangesOpen(false);
          setDeviationOpen(true);
          setActiveTab('deviation');
        }

        const results = data.results || [];
        const newDeviationIds = [];

        results.forEach(result => {
          const devKey = "type: Deviation";
          const devInfo = result[devKey];

          if (devInfo && devInfo.deviationid) {
            const deviationIds = devInfo.deviationid.includes("|")
              ? devInfo.deviationid.split("|")
              : [devInfo.deviationid];
            newDeviationIds.push(...deviationIds);
          }
        });

        
        setdevObjectIds(newDeviationIds);
      } else {
        console.error('Error fetching data:', response.status);
      }
    } catch (error) {
      console.error('Error:', error);
    }
  };

  if (searchTerm && selectedOption && searchTerm.length > 3) {
    fetchDataForDeviation();
  }
}, [searchTerm, selectedOption]);



useEffect(() => {
  const fetchDataForParts = async () => {
    try {
      const response = await fetch("http://localhost:8081/Supplierportal/webapi/SupplierPortal/search", {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          text: searchTerm,
          field: selectedOption,
        }),
      });

      if (response.ok) {
        const data = await response.json();


        if (data.results && data.results.length > 0) {
          setAssignedParts(true);
          setChangesOpen(false);
          setDeviationOpen(false);
          setActiveTab('assignedParts');
        }

        const results = data.results || [];
        const newPartIds = [];
        results.forEach(result => {
          const partKey = "type: Part";
          const partInfo = result[partKey];
          setType(partKey);

          if (partInfo && partInfo.partid) {
            const partIds = partInfo.partid.includes("|")
              ? partInfo.partid.split("|")
              : [partInfo.partid];
            newPartIds.push(...partIds);
          }
        });

        setObjectIds(newPartIds);
      } else {
        console.error('Error fetching data:', response.status);
      }
    } catch (error) {
      console.error('Error:', error);
    }
  };

  if (searchTerm && selectedOption && searchTerm.length > 3) {
    fetchDataForParts();
  }
}, [searchTerm, selectedOption]);

// Function to fetch assigned parts visibility and only store partId if visibility is true
useEffect(() => {
    const fetchVisibility = async (partId) => {
      
      
        try {
            const visibilityResponse = await fetch(
                `http://localhost:8081/Supplierportal/webapi/SupplierPortal/getAssignedPartsVisibility?suppliername=${supplierName}&partid=${partId}`
            );
            
            if (visibilityResponse.ok) {
                const visibilityData = await visibilityResponse.json();
               
                const visibilityResult = visibilityData.results[0];
             
                
                // Ensure visibility is checked correctly
                if (visibilityResult && visibilityResult.Visibility === 'true') {
                    const id = visibilityResult.id; // Extract the id
                    const specVisibility =visibilityResult.specVisibility;
                    const itemVisibility =visibilityResult.itemVisibility;
                    setVisiblePartIds(prev => [...prev, partId]);
                    setvisibleSpec(prev => [...prev, specVisibility]);
                    setVisibleItem(prev => [...prev, itemVisibility])
                    
                } else { 
                    
                    console.log(`Part ID ${partId} is not visible.`);
                }
            } else {
                console.error(`Error fetching visibility for Part ID ${partId}:`, visibilityResponse.status);
            }
        } catch (error) {
            console.error(`Error fetching visibility for Part ID ${partId}:`, error);
        }
    };

    if (objectIds.length > 0) {
        objectIds.forEach(partId => {
            fetchVisibility(partId);
        });
    }
}, [objectIds, supplierName]);





  return (
    <div>
      <FirstContext.Provider value={{
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
      }}>
        <Nav 
          searchTerm={searchTerm} 
          handleSearchChange={handleSearchChange} 
          searchList={searchList} 
          selectedOption={selectedOption} 
          handleOptionClick={setSelectedOption} 
          handleSearchList={() => showSearchList(prev => !prev)}
        />
        {/* Conditional rendering for MainSection, Changes_Page, and Deviation_Page */}
        {assignedParts && (
          <MainSection selectedRow={selectedRow} setSelectedRow={setSelectedRow} setSupplierName={setSupplierName} visiblePartIds={visiblePartIds} visibleSpec={visibleSpec} visibleItem={visibleItem}/>
        )}
        {changesOpen && (
          <Changes_Page selectedRData={selectedRow} setSelectedRData={setSelectedRow} caObjectIds={caObjectIds}/>
        )}
        {deviationOpen && (
          <Devaition_Page devObjectIds={devObjectIds}/>
        )}

        <SessionTimeoutDialog />
      </FirstContext.Provider>
    </div>
  );
};

// PrivateRoute component to handle protected routes
const PrivateRoute = ({ element }) => {
  const jwt = (JSON.parse(localStorage.getItem('userData')) && JSON.parse(localStorage.getItem('userData')).jwt) || false;

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
