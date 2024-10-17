import { useContext, useState, useEffect, useRef } from "react";
import "../Styles/Nav.css";
import { useNavigate } from "react-router-dom"; // Import useNavigate

import { FirstContext } from "../App";
import GoogleTranslate from "./GoogleTranslate";

export const Nav = ({ searchTerm, handleSearchChange, searchList, selectedOption, handleOptionClick, handleSearchList }) => {
    const { selectedTheme } = useContext(FirstContext);

    const getNavBarClass = () => {
        switch (selectedTheme) {
            case "Dark theme":
                return "nav_dark_theme";
            case "Light theme":
                return "nav_light_theme";
            case "Default":
                return "nav_default_theme";
            default:
                return "nav_default_theme";
        }
    };

    return (
        <div className={`nav_bar_conatiner ${getNavBarClass()}`}>
            <Logo />
            <Nav_Tabs
                searchTerm={searchTerm}
                handleSearchChange={handleSearchChange}
                searchList={searchList}
                selectedOption={selectedOption}
                handleOptionClick={handleOptionClick}
                handleSearchList={handleSearchList}
            />
            <MenuSection />
        </div>
    );
};

const Logo = () => {
    return (
        <div className="logo_container">
            <a href=""><img src="assets/xp_logo.png" alt="Xploria" /></a>
        </div>
    );
};

export const Nav_Tabs = ({ searchTerm, handleSearchChange, searchList, selectedOption, handleOptionClick, handleSearchList }) => {
    const { handleChangePage, handleAssignPart, handleDeviationPage, activeTab, setActiveTab  } = useContext(FirstContext);
    const [changeActionCount, setChangeCount] = useState(null);
    const [deviationCount, setDevCount] = useState(null);
    const [partCount, setPartCount] = useState(null);
    // const [activeTab, setActiveTab] = useState('assignedParts');
    const [temporarySearchTerm, setTemporarySearchTerm] = useState(searchTerm); // Temporary state for the search input
    const username = localStorage.getItem('username');

    useEffect(() => {
        fetch(`http://localhost:8081/Supplierportal/webapi/SupplierPortal/getCount?email=${username}`)
            .then(response => response.json())
            .then(data => {
                const result = data.results[0];
                setDevCount(result.deviation);
                setChangeCount(result.changeaction);
                setPartCount(result.ecparts);
            })
            .catch(error => console.error('Fetch error:', error));
    }, [username]);

    const handleTabClick = (tab, action) => {
        setActiveTab(tab);
        action(); // Execute the action associated with the tab
    };

    const handleSearchSubmit = () => {
        // Check the length of the search term
        if (temporarySearchTerm.length < 3) {
            alert('Search term must be at least 3 characters long.');
            setTemporarySearchTerm('');
            return; // Prevent further processing
        }

        handleSearchChange(temporarySearchTerm); // Execute search logic
    };

    const handleClearSearch = () => {

        setTemporarySearchTerm(''); // Clear the temporary search term
        handleSearchChange(''); // Pass an empty string to searchTerm
        
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault(); // Prevent form submission
            handleSearchSubmit(); // Trigger search
        }
    };

    const handleOptionClickWithClose = (option) => {
        handleOptionClick(option);
        handleSearchList(false); // Close the dropdown after option selection
    };
    

    return (
        <div className="nav_tab_container">
            <div className="search_section">
                <div className="search_dropdown">
                    <div className="search_dropdown_text" onClick={handleSearchList}>
                        <span>{selectedOption}</span>
                        <ion-icon name="chevron-down-outline" id={`${searchList ? "rotate_icon" : ""}`}></ion-icon>
                    </div>
                    <ul className={`search_dropdown_list ${searchList ? "search_list_show" : ""}`}>
                        {['Everything', 'Name', 'IDs'].map(option => (
                            <li
                                key={option}
                                className="search_dropdown_item"
                                onClick={() => handleOptionClickWithClose(option)}
                            >
                                {option}
                            </li>
                        ))}
                    </ul>
                </div>

                <div className="search_box">
                    <input
                        id="search-input"
                        type="text"
                        placeholder="Search anything..."
                        value={temporarySearchTerm} // Bind to temporary search term
                        onChange={(e) => setTemporarySearchTerm(e.target.value)} // Update temporary term on input change
                        onKeyDown={handleKeyDown} // Trigger search on Enter key
                    />
                    {/* X icon to clear search */}
                    {temporarySearchTerm && ( // Only display if there is text in the search box
                        <ion-icon
                            name="close-outline"
                            className="clear_search_icon"
                            title="Clear" 
                            onClick={handleClearSearch} // Clear search on click
                        ></ion-icon>
                    )}
                    <ion-icon
                        name="search-outline"
                        title="Search" 
                        onClick={handleSearchSubmit} // Trigger search on icon click
                    ></ion-icon>
                </div>
            </div>

            <div className="nav_links">
                <a
                    href="#"
                    className={activeTab === 'assignedParts' ? 'active_link' : ''}
                    onClick={() => handleTabClick('assignedParts', handleAssignPart)} >
                    Assigned Parts({partCount})
                </a>
                <a
                    href="#"
                    className={activeTab === 'changes' ? 'active_link' : ''}
                    onClick={() => handleTabClick('changes', handleChangePage)} >
                    Changes({changeActionCount})
                </a>
                <a
                    href="#"
                    className={activeTab === 'deviation' ? 'active_link' : ''}
                    onClick={() => handleTabClick('deviation', handleDeviationPage)} >
                    Deviation({deviationCount})
                </a>
            </div>
        </div>
    );
};


const MenuSection = () => {
    const { selectedTheme, setSelectedTheme } = useContext(FirstContext);
    const [showProfileMenu, setShowProfileMenu] = useState(false);
    const [showLangMenu, setShowLangMenu] = useState(false);
    const [showThemeMenu, setShowThemeMenu] = useState(false);
    const profileMenuRef = useRef(null);
    const langMenuRef = useRef(null);
    const themeMenuRef = useRef(null);
    const navigate = useNavigate(); // Use useNavigate for routing
    const handleProfileMenu = () => {
        setShowProfileMenu((pre) => !pre);
        setShowLangMenu(false);
        setShowThemeMenu(false);
    };
    const handleLangMenu = () => {
        setShowLangMenu((pre) => !pre);
        setShowProfileMenu(false);
    };
    const handleThemeMenu = () => {
        setShowThemeMenu((pre) => !pre);
        setShowProfileMenu(false);
    };
    const handleBackbtn = () => {
        setShowProfileMenu(true);
        setShowLangMenu(false);
        setShowThemeMenu(false);
    };

    const handleThemeChange = (theme) => {
        setSelectedTheme(theme);
    };

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (
                profileMenuRef.current && !profileMenuRef.current.contains(event.target) &&
                langMenuRef.current && !langMenuRef.current.contains(event.target) &&
                themeMenuRef.current && !themeMenuRef.current.contains(event.target)
            ) {
                setShowProfileMenu(false);
                setShowLangMenu(false);
                setShowThemeMenu(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    const themsData = ["Default", "Dark theme", "Light theme"];
    const handleLogout = () => {
        localStorage.removeItem("username"); // Clear user details from local storage
        navigate("/login"); // Redirect to login page
      };
    return (
        <>
            <div className="menu_icon_circle" onClick={handleProfileMenu}>
                <img src="assets/user2.jpg" alt="" />
            </div>
            <div ref={profileMenuRef} className={`dropdown_container ${showProfileMenu ? "dropdown_active" : ''}`}>
                <div className="user_banner"></div>
                <div className="user_img">
                    <img src="assets/user2.jpg" alt="" />
                </div>
                <div className="user_data">
                    <p className="user_name">Aswath Venkatesan</p>
                    <p className="user_role">Senior Developer</p>
                    <a href=""><i className="fa-regular fa-pen-to-square profile_edit_icon"></i></a>
                </div>
                <div className="profile_settings">
                    <a className="settings" href="#" onClick={handleLangMenu}><li><ion-icon name="language-outline"></ion-icon>Language<span></span></li><ion-icon name="chevron-forward-outline"></ion-icon></a>
                    <a className="settings" href="#" onClick={handleThemeMenu}><li><ion-icon name="contrast-outline"></ion-icon>Theme:<span>{selectedTheme}</span></li><ion-icon name="chevron-forward-outline"></ion-icon></a>
                    <a className="settings" href="#"><li><ion-icon name="bowling-ball-outline"></ion-icon>Clear Cookie</li></a>
                    <a className="settings" href="#"><li><ion-icon name="help-outline"></ion-icon>FAQ</li></a>
                    <a className="settings" href="#" onClick={handleLogout}><li><ion-icon name="log-out-outline"></ion-icon>Logout </li></a>
                    {/* <a className="settings" href="#"><li><ion-icon name="log-out-outline"></ion-icon>Logout </li></a> */}
                </div>
            </div>

            {/* Language Settings */}
            <div ref={langMenuRef} className={`lang_menu_section ${showLangMenu ? 'lang_active' : ''}`}>
                <div className="back_btn" onClick={handleBackbtn}>
                    <ion-icon name="chevron-back-outline"></ion-icon>
                </div>
                <div className="language_menu">
                    <h2>Select Language</h2>
                    <GoogleTranslate />
                </div>
            </div>

            {/* Theme Settings */}
            <div ref={themeMenuRef} className={`theme_menu_section ${showThemeMenu ? 'theme_active' : ''}`}>
                <div className="back_btn" onClick={handleBackbtn}>
                    <ion-icon name="chevron-back-outline"></ion-icon>
                </div>
                <h2>Select Theme</h2>
                <ul className="theme_menu">
                    {themsData.map((theme, index) => (
                        <li key={index} onClick={() => handleThemeChange(theme)}>
                            {theme}
                        </li>
                    ))}
                </ul>
            </div>
        </>
    );
};


export default Nav;