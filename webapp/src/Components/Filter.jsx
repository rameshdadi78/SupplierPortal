import "../Styles/Filter.css";
import API_BASE_URL from '../config';
import { MainContext } from "./MainSection";
import columnOrderConfig from './columnOrderConfig.json';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { useNavigate } from "react-router-dom";
import { useContext, useEffect } from "react";
import { useState,useRef } from "react";
import { CSVLink } from 'react-csv';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';
import { FirstContext } from "../App";
import { DownloadSnackbar } from "./DownloadSnackbar";
const MySwal = withReactContent(Swal);



export const Filter = ({ columnDisplayNames, onFilter, onSearch, filterText, filterValue, filterColumn, filterOperator, data, filteredData, rowsPerPage, exportSpecificRows,filterDataForExport,generateCsvHeaders,userPre,setUserPre ,setCurrentViewData,currentViewData,columns={columns}}) => {


    const clearFilters = () => {
        onFilter({ column: '', operator: 'contains', value: '' });
    };

    const csvHeaders = generateCsvHeaders(columnDisplayNames);
    const filteredExportData = filterDataForExport(filteredData,currentViewData);
    const allDataExport = filterDataForExport(data,currentViewData);

   
 


return (
        <div className='whole_menu_bar'>
            <Views userPre={userPre} setUserPre={setUserPre} setCurrentViewData={setCurrentViewData} currentViewData={currentViewData}columnDisplayNames={columnDisplayNames} />
            <FilterField filterColumn={filterColumn} onFilter={onFilter} clearFilters={clearFilters} filterOperator={filterOperator} filterValue={filterValue} columnDisplayNames={columnDisplayNames} currentViewData={currentViewData}/>     
            <Export csvHeaders={csvHeaders} filteredExportData={filteredExportData} allDataExport={allDataExport} rowsPerPage={rowsPerPage} exportSpecificRows={exportSpecificRows} currentViewData={currentViewData}/>
            <Search onSearch={onSearch} filterText={filterText}/>
        </div>
    );
};



export const Views = ({userPre,setCurrentViewData,currentViewData,setUserPre,columnDisplayNames}) => {
  const {sessionTimeoutOpen,setSessionTimeoutOpen} = useContext(FirstContext);
  
let ViewsArr =userPre.views.main_table_view;  //[{view1 data},{view2 data},{view3 data}]
const [viewName,setViewName]=useState('System View')
const  [viewData, setViewData] = useState(ViewsArr[0].name);
// console.log(viewData);

// console.log(userPre);

const [editingData, setEditingData] = useState(currentViewData || []);
// console.log(editingData);




const handleViewChange = (newViewName) => {
  setViewName(newViewName);
  const selectedView = userPre.views.main_table_view.find(view => view.view_name === newViewName);
  if (selectedView) {
      // Call the handleViewChange function in MainSection via props or context
      setCurrentViewData(selectedView.name);
      setEditingData(selectedView.name);
     
  }
};

let AvailableBeforeOrder = userPre.main_table.name;
let VisibleBeforeOrder = userPre.views.main_table_view[0].name;

const sortColumns = (columns) => {
  return columns.sort((a, b) => {
    return columnOrderConfig.columnOrder.indexOf(a) - columnOrderConfig.columnOrder.indexOf(b);
  });
};
// Sort AvailableColumns and VisibleColumns
const AvailableColumns = sortColumns(AvailableBeforeOrder);
const VisibleColumns = sortColumns(VisibleBeforeOrder)

// -------------------------------popup-------------------------
const [tblviewPopup, setTblviewPopup] = useState(false);
const [isEditDropdownOpen, setIsEditDropdownOpen] = useState(false);
const popupRef = useRef(null); 
const buttonRef = useRef(null); // Reference to the "Table View" button
const editDropdownRef = useRef(null); 

  const handleTableviewSection = () => {
    setTblviewPopup(prev => !prev); 
  };
  const handleEditOpen = () => {
    setIsEditDropdownOpen(prev => !prev);
  };
  const handleClickOutside = (event) => {
    // Close main popup if clicked outside of it and not inside the edit dropdown
    if (
      popupRef.current && 
      !popupRef.current.contains(event.target) && 
      buttonRef.current && 
      !buttonRef.current.contains(event.target) &&
      !editDropdownRef.current?.contains(event.target) // Exclude the sub-menu (Edit View)
    ) {
      setTblviewPopup(false);
    }

    // Close Edit Current Table View dropdown if clicked outside of it
    if (
      editDropdownRef.current && 
      !editDropdownRef.current.contains(event.target)
    ) {
      setIsEditDropdownOpen(false);
    }
  };

  useEffect(() => {
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);
// ------------------------------------------------------------------



const [createHandleTbl,setCreateHandleTbl] =useState(false)
const handleCreatTblOpen=()=>{
   setCreateHandleTbl(true);
}
const handleCloseCreTbl=()=>{
  setCreateHandleTbl(false)
}



//Delete web service
const handleDeleteView = async (viewName) => {
  localStorage.setItem("is_Active","true")

  if (viewName === "System View") {
      // MySwal.fire('Warning!', 'You cannot delete the system view.', 'warning');
      toast.error("You cannot delete the system view!");
      return;
  }

  const result = await MySwal.fire({
      title: 'Are you sure?',
      text: `Do you want to delete the view "${viewName}"?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Yes, delete it!',
      cancelButtonText: 'Cancel',
      reverseButtons: true,
  });

  if (result.isConfirmed) {
      const jwtToken = JSON.parse(localStorage.getItem('userData')).jwt;
      const storedUserName = localStorage.getItem('username');

      try {
         const response = await fetch(`${API_BASE_URL}/myresource/deleteView`, {
              method: "POST",
              headers: {
                  "Content-Type": "application/json",
                  Accept: "application/json",
                  jwt: jwtToken,
              },
              body: JSON.stringify({
                  username: storedUserName,
                  view_name: viewName,
              }),
          });

          if (response.status === 401) {
            setSessionTimeoutOpen(true)
            return;
          }
          if (response.ok) {
              const updatedPreference = await response.json();
              localStorage.setItem('userData', JSON.stringify({ ...JSON.parse(localStorage.getItem('userData')), preference: updatedPreference }));
              setUserPre(prev => ({
                  ...prev,
                  views: {
                      ...prev.views,
                      main_table_view: updatedPreference.views.main_table_view
                  }
              }));
   
                  setViewName("System View");
                  handleViewChange("System View")
                  toast.success("View deleted successfully");
             
          } else {
              throw new Error('Failed to delete the view.');
          }
      } catch (error) {
          MySwal.fire('Error!', 'There was a problem with the deletion.', 'error');

      }
  }
};


    
  return (
<div className='tbl_view_section'>
  <div ref={buttonRef} onClick={handleTableviewSection}>
    <div className='top_setting' ><i title="Filter By Column Selection"  class="ri-table-line menu_icons" ></i><span>Table View</span><i class="ri-arrow-down-s-fill"></i></div>
  </div>
    {/* DropDown */}
    
    <div  ref={popupRef} className={`tbl_view_popup_container ${tblviewPopup?'open_tbl_view_popup':''}`}>
      <div className='tbl_view_settings'>
        <div className='tbl_view_setting_heading'> <i class="ri-tools-line"></i>Settings</div>
        <div className='tbl_options'>
            <label  className='tbl_option_lbl' onClick={handleCreatTblOpen}><i class="ri-add-circle-line create_view"></i><span>Create New Table View</span></label>  
            <label  className='tbl_option_lbl tbl_edit_lbl' onClick={handleEditOpen}><i class="ri-pencil-line edit_view" ></i><span>Edit Current Table View</span><i class={`ri-arrow-down-s-fill ${isEditDropdownOpen?'edit_view_arrow':''}`}></i> </label>  
            <label  className='tbl_option_lbl'  onClick={() => handleDeleteView(viewName)}><i class="ri-delete-bin-6-line delete_view"></i><span>Delete Current Table View</span></label>    
         </div>
      </div>

      <ViewDisplaySec ViewsArr={ViewsArr} viewName={viewName} handleViewChange={handleViewChange}/>

    </div>
    <EditView AvailableColumns={AvailableColumns} viewName={viewName} currentViewData={currentViewData}isEditDropdownOpen={isEditDropdownOpen} setIsEditDropdownOpen={setIsEditDropdownOpen} editingData={editingData} setEditingData={setEditingData}setCurrentViewData={setCurrentViewData} setUserPre={setUserPre} columnDisplayNames={columnDisplayNames} editDropdownRef={editDropdownRef} setTblviewPopup={setTblviewPopup}/>
    <CreateView AvailableColumns={AvailableColumns}VisibleColumns={VisibleColumns} createHandleTbl={createHandleTbl} handleCloseCreTbl={handleCloseCreTbl} setUserPre={setUserPre} setCreateHandleTbl={setCreateHandleTbl} userPre={userPre} setViewName={setViewName} setCurrentViewData={setCurrentViewData} setEditingData={setEditingData}handleViewChange={handleViewChange}columnDisplayNames={columnDisplayNames}/>
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
        // style={{ top:'15%'}}
      />   
</div>

  )
}



  export const CreateView = ({ AvailableColumns, VisibleColumns, createHandleTbl, handleCloseCreTbl,setUserPre,setCreateHandleTbl,userPre,setViewName,setCurrentViewData,setEditingData,handleViewChange,columnDisplayNames}) => {
    const {sessionTimeoutOpen,setSessionTimeoutOpen} = useContext(FirstContext);
    const navigate = useNavigate();

    const [newviewName, setNewViewName] = useState('');
    const [viewNameError, setViewNameError] = useState('');
  
    const [availableCol, setAvailableCol] = useState(AvailableColumns || []);
    const [visibleCol, setVisibleCol] = useState(VisibleColumns || []);
  
    const [selectedAvailable, setSelectedAvailable] = useState([]);
    const [selectedVisible, setSelectedVisible] = useState([]);
  
    useEffect(() => {
      setAvailableCol(AvailableColumns);
      setVisibleCol(VisibleColumns);
    }, [AvailableColumns, VisibleColumns]);
  
    const handleSelectAvailable = (column) => {
      setSelectedAvailable(prev =>
        prev.includes(column) ? prev.filter(clm => clm !== column) : [...prev, column]
      );
    };
  
    const handleSelectVisible = (column) => {
      if (column !== 'type') {
        setSelectedVisible(prev =>
          prev.includes(column) ? prev.filter(clm => clm !== column) : [...prev, column]
        );
      }
    };
  
    const moveToVisible = () => {
      const newVisibleCol = [...visibleCol, ...selectedAvailable.filter(clm => !visibleCol.includes(clm))];
      setVisibleCol(newVisibleCol);
      // setAvailableCol(availableCol.filter(clm => !selectedAvailable.includes(clm)));
      setSelectedAvailable([]);
    };
  
    const moveToAvailable = () => {
      const newAvailableCol = [...availableCol, ...selectedVisible.filter(clm => !availableCol.includes(clm))];
      setAvailableCol(newAvailableCol);
      setVisibleCol(visibleCol.filter(clm => !selectedVisible.includes(clm)));
      setSelectedVisible([]);
    };
  
    const emptyVisible = () => {
      setVisibleCol(['type']);
    };
  
    const handleResrore =()=>{
      setVisibleCol(VisibleColumns)
    }


  const handleCreateView = async () => {
    localStorage.setItem("is_Active","true")
    const jwtToken =JSON.parse(localStorage.getItem('userData')).jwt;  
  const storedUserName = localStorage.getItem('username');

  const trimmedViewName = newviewName.trim();

  // Validate view name
  if (!trimmedViewName) {
    setViewNameError("View name is required");
    return;
  }


  const viewExists = userPre.views.main_table_view.some(view => view.view_name === trimmedViewName);
if (viewExists) {
  setViewNameError("This view name is already present");
  return;
}

  setViewNameError(''); // Clear any existing error message

  // Prepare preference data
  const preferenceData = {
    view_name: trimmedViewName,
    name: visibleCol, 
    display: visibleCol.map(clm =>  clm), 
    default: "false", 
  };
// console.log(preferenceData);
  try {
   const response = await fetch(`${API_BASE_URL}/myresource/updatePre`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          "jwt": jwtToken,
        },
        body: JSON.stringify({
          username: storedUserName,
          preference: preferenceData,
        }),
      }
    );

    if (response.status === 401) {
      setSessionTimeoutOpen(true)
      return;
    }
    // Check for a successful response
    if (!response.ok) {
      throw new Error('Failed to create the view. Please try again.');
    }

    const updatedPreference = await response.json();
    // console.log(updatedPreference);

    let userData = JSON.parse(localStorage.getItem('userData'));
    userData.preference = updatedPreference;
    localStorage.setItem('userData', JSON.stringify(userData));
    // console.log("Updated userData:", userData);
    const updatedViewsArr = userData.preference.views.main_table_view;

    setUserPre(prev => ({
      ...prev,
      views: {
        ...prev.views,
        main_table_view: updatedViewsArr
      }
    }));
   
    
    setViewName(trimmedViewName);
    setCurrentViewData(updatedViewsArr.find(view => view.view_name === trimmedViewName)?.name || []);
    setEditingData(updatedViewsArr.find(view => view.view_name === trimmedViewName)?.name || []);

 
    toast.success("View creation was successful");

    setCreateHandleTbl(false);
    setNewViewName('');


  } catch (error) {
    console.error("There was a problem with the fetch operation:", error);

    // Show error message
    MySwal.fire({
      title: 'Error!',
      text: 'There was a problem creating the view. Please try again later.',
      icon: 'error',
      confirmButtonText: 'OK',
    });
  }
};

// ---------Modern dialog scroll hidden
useEffect(() => {
  if (createHandleTbl) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = 'auto';
  }
  return () => {
    document.body.style.overflow = 'auto';
  };
}, [createHandleTbl]);
    return (
      <>
        <div className={`overlay ${createHandleTbl ? 'visible' : ''}`}></div>
        <div className={`cre_tbl_view_container ${createHandleTbl ? 'open_cre_tbl' : ''}`} >
  
          <div className='cre_tbl_top_sec'>
            <p className='cre_tbl_title'><i className="ri-eye-2-line"></i>Customize Table View</p>
            <i className="fa-solid fa-xmark close_cre_tbl" onClick={handleCloseCreTbl}></i>
          </div>
  
          <div className='cre_tbl_new_sec'>
            <p>Name:</p>
            <input
              type="text"
              value={newviewName}
              onChange={e => setNewViewName(e.target.value)}
            />
            {viewNameError && <p style={{ color: 'red' }}>{viewNameError}</p>}
          </div>
  
          <div className='cre_tbl_content_sec'>
  
            <div className='available_container'>
              <div className="available_table">
                <p>Available Columns</p>
                <div className='available_table_box'>
                  {availableCol.map((clm) => (
                    <label key={clm}>
                      <ion-icon name={selectedAvailable.includes(clm) ? "checkmark-circle" : "ellipse-outline"}></ion-icon>
                      <input type="checkbox" checked={selectedAvailable.includes(clm)} onChange={() => handleSelectAvailable(clm)} />
                      {columnDisplayNames[clm] || clm}
                    </label>
                  ))}
                </div>
              </div>
              <div className='available_settings'>
                <button onClick={moveToAvailable}><i className="ri-arrow-left-s-line"></i></button>
                <button onClick={moveToVisible}><i className="ri-arrow-right-s-line"></i></button>
                <button onClick={emptyVisible}><i className="ri-arrow-left-double-line"></i></button>
              </div>
            </div>
  
            <div className='visible_container'>
              <div className="visible_table">
                <p>Visible Columns</p>
                <div className='visible_table_box'>
                  {visibleCol.map((clm) => (
                    <label key={clm}>
                      <ion-icon name={selectedVisible.includes(clm) ? "checkmark-circle" : "ellipse-outline"}></ion-icon>
                      <input type="checkbox" checked={selectedVisible.includes(clm)} onChange={() => handleSelectVisible(clm)} />
                      {columnDisplayNames[clm] || clm}
                    </label>
                  ))}
                </div>
              </div>
              <div className='visible_settings'>
                <button><i className="ri-arrow-up-s-line"></i></button>
                <button><i className="ri-arrow-down-s-line"></i></button>
              </div>
            </div>
  
          </div>
  
          <div className='cre_tbl_bottom_sec'>
            <button onClick={handleResrore} className='reset_view_btn'>Reset to Default <i className="ri-refresh-line"></i></button>
            <button onClick={handleCreateView} className='create_view_btn'>Create View <i className="fa-regular fa-circle-check"></i></button>
          </div>
        </div>
  
      </>
    );
  };



  export const EditView = ({AvailableColumns,viewName,currentViewData,isEditDropdownOpen,editingData,setEditingData,setCurrentViewData,setIsEditDropdownOpen,setUserPre,columnDisplayNames,editDropdownRef,setTblviewPopup}) => {
    const {sessionTimeoutOpen,setSessionTimeoutOpen} = useContext(FirstContext);
  

    const handleResetView = () => {
      setEditingData(currentViewData); 
    };
    // console.log(currentViewData);



    const handleEditView = async (viewName) => {
      localStorage.setItem("is_Active","true")

      if (viewName === "System View") {
        toast.error("System View is not Edittable!!");
        return;
      }

      const jwtToken =JSON.parse(localStorage.getItem('userData')).jwt;  
      // console.log(jwtToken);
      const storedUserName = localStorage.getItem('username');
  
      const preferenceData = {
        view_name: viewName,
        name: editingData,
        display: editingData.map(clm => clm),  
        default: 'false',
      };
  
      try {
      const response = await fetch(`${API_BASE_URL}/myresource/edit`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
              "jwt": jwtToken,
            },
            body: JSON.stringify({
              username: storedUserName,
              preference: preferenceData,
            }),
          }
        );
          
        if (response.status === 401) {
          setSessionTimeoutOpen(true)
          return;
        }
        if (response.ok) {
        const updatedPreference = await response.json();
        let userData = JSON.parse(localStorage.getItem('userData'));
        userData.preference = updatedPreference;
        localStorage.setItem('userData', JSON.stringify(userData));

  
        setCurrentViewData(editingData);
  
        const updatedViewsArr = userData.preference.views.main_table_view;
        setUserPre(prev => ({
          ...prev,
          views: {
            ...prev.views,
            main_table_view: updatedViewsArr
          }
        }));
        setIsEditDropdownOpen(false)
        setTblviewPopup(false)
        
        toast.success("Editing completed successfully!");
      }
      } catch (error) {
        console.error("There was a problem with the fetch operation:", error);
      }
    };
  
   
    // console.log(columnDisplayNames);
    const handleEditdCol = (column) => {
      if (column === "type") return; 
  
      setEditingData((prevData) => {
        if (prevData.includes(column)) {
          return prevData.filter((clm) => clm !== column);
        } else {
          return [...prevData, column];
        }
      });
    };
   
      return (
        <div ref={editDropdownRef} className={`edit_view_tbl_dropdown ${isEditDropdownOpen ? 'openEditTbl' : ''}`}>   
            <div  className='edit_view_tbl_btns'>
                  <button className='reset_btn_view_tbl' onClick={handleResetView}>Reset <i class="ri-loop-left-line"></i></button>
                  <button className='save_btn_view_tbl' onClick={()=>handleEditView(viewName)}>Save <i class="ri-save-line"></i></button>
            </div>
            <div className="edit_view_tbl_dropdown_list">
              {AvailableColumns.map((clm) => (
                     <label key={clm} className="editing_section_lbl">
                     <ion-icon name={editingData.includes(clm) ? "checkmark-circle" : "ellipse-outline"}></ion-icon>
                     <input className='edit_tble_input'
                       type="checkbox" 
                       checked={editingData.includes(clm)} 
                       onChange={() => handleEditdCol(clm)} 
                     />
                     {columnDisplayNames[clm]||clm}
                   </label>
              ))} 
            </div>                                                     
      </div>
      )
    }
    


export const ViewDisplaySec = ({ViewsArr,viewName,handleViewChange}) => {
  return (
    <div className='created_tbl_viwes'>
        <div className='tbl_view_setting_heading'><i class="ri-eye-2-line"></i>Table Views</div>
        <div className='tbl_options'>
        {ViewsArr.map((view) => (
                <label key={view.view_name} className='tbl_option_lbl'>
                  <input type="checkbox" checked={viewName === view.view_name} onChange={() => handleViewChange(view.view_name)} />
                  <ion-icon name="checkmark-outline"></ion-icon>
                  <span>{view.view_name}</span>
                </label>
              ))}
        </div>
      </div>
  )
}










// FilterField Component
export const FilterField = ({filterColumn,onFilter,clearFilters,filterOperator,filterValue,columnDisplayNames,currentViewData}) => {

// -------------------------------popup-------------------------
const [isFilterPopupOpen, setIsFilterPopupOpen] = useState(false);
const popupRef = useRef(null); 
  const buttonRef = useRef(null); // Reference to the "Table View" button

  const toggleFilterPopup = () => {
    setIsFilterPopupOpen(prev => !prev);
};

  const handleClickOutside = (event) => {
    if (
      popupRef.current && 
      !popupRef.current.contains(event.target) && 
      buttonRef.current && 
      !buttonRef.current.contains(event.target)
    ) {
      setIsFilterPopupOpen(false);
    }
  };

  useEffect(() => {
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);
// ------------------------------------------------------------------

  const filteredColumnDisplayNames = Object.entries(columnDisplayNames).filter(([key]) => currentViewData.includes(key));

  // console.log(filterColumn);
  return (
    <div className='filter_section'>
                    <div ref={buttonRef} className='top_setting' onClick={toggleFilterPopup} >
                    <i title="Filter By Column Selection" className="ri-filter-2-line menu_icons"></i>
                    <span>Filter</span>
                    <i className="ri-arrow-down-s-fill"></i>
                </div>
                {isFilterPopupOpen && (
                    <div  ref={popupRef} className="filter-popup">
                        <div className='filter_popup_item'>
                            <label className='filter_dropdown_lbl'>Columns:</label>
                            <i className="ri-arrow-down-s-fill down_select"></i>
                            <select className="custom-select" value={filterColumn} onChange={e => onFilter({ column: e.target.value, operator: filterOperator, value: filterValue })}>
                                 {filteredColumnDisplayNames.map(([key, displayName]) => (
                                        <option key={key} value={key}>{displayName}</option>
                                  ))}
                            </select>
                        </div>

                        <div className='filter_popup_item'>
                            <label className='filter_dropdown_lbl'>Operator:</label>
                            <i className="ri-arrow-down-s-fill down_select"></i>
                            <select className="custom-select" value={filterOperator} onChange={e => onFilter({ column: filterColumn, operator: e.target.value, value: filterValue })}>
                                <option value="contains">Contains</option>
                                <option value="equals">Equals</option>
                                <option value="startsWith">Starts With</option>
                            </select>
                        </div>

                        <div className='filter_popup_item'>
                            <label className='filter_dropdown_lbl'>Value:</label>
                            <input type="text" value={filterValue} onChange={e => onFilter({ column: filterColumn, operator: filterOperator, value: e.target.value })} />
                        </div>

                        <div className="filter_actions">
                            <button onClick={clearFilters}>Reset</button>
                        </div>
                    </div>
                )}
            </div>
  )
}







// Export Component
export const Export = ({ filteredExportData, allDataExport, csvHeaders, exportSpecificRows, rowsPerPage, currentViewData }) => {

  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState("");
  const expPopupRef = useRef();

 
// -------------------------------popup-------------------------
const [expPopup, openExpPopup] = useState(false);
const popupRef = useRef(null); 
  const buttonRef = useRef(null); // Reference to the "Table View" button

  const toggleExpPopup = () => {
    openExpPopup(!expPopup);
  };


  const handleClickOutside = (event) => {
    if (
      popupRef.current && 
      !popupRef.current.contains(event.target) && 
      buttonRef.current && 
      !buttonRef.current.contains(event.target)
    ) {
      openExpPopup(false);
    }
  };

  useEffect(() => {
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);
// ------------------------------------------------------------------


  // ---------Snackbar section
  const handleSnackbarOpen = (message) => {
    setSnackbarMessage(message);
    setSnackbarOpen(true);
  };

  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setSnackbarOpen(false);
  };
// -----------------------------


  const filterDataByCurrentView = (data) => {
    if (!currentViewData || !Array.isArray(currentViewData)) return [];
    return data.map(item => {
      const filteredItem = {};
      currentViewData.forEach(key => {
        if (item.hasOwnProperty(key)) {
          filteredItem[key] = item[key];
        }
      });
      return filteredItem;
    });
  };

  const getFilteredHeaders = (csvHeaders, currentViewData) => {
    return csvHeaders.filter(header => currentViewData.includes(header.key));
  };

  return (
    <div ref={buttonRef} className="export_section" onClick={toggleExpPopup}>
      <div className="top_setting">
        <i className="ri-file-download-line menu_icons"></i>
        <span>Export</span>
        <i className="ri-arrow-down-s-fill"></i>
      </div>

      <div ref={popupRef} className={`export_popup ${expPopup ? 'open_export_popup' : ''}`}>
        <div className="export_all">
          <CSVLink
            data={filterDataByCurrentView(allDataExport)}
            headers={getFilteredHeaders(csvHeaders, currentViewData)}
            filename="all_data.csv"
            onClick={() => handleSnackbarOpen("Export All data downloaded")}
          >
            <div className="export_option">Export All Rows</div>
          </CSVLink>
        </div>
        <div className="export_filter">
          <CSVLink
            data={filterDataByCurrentView(filteredExportData)}
            headers={getFilteredHeaders(csvHeaders, currentViewData)}
            filename="filtered_data.csv"
            onClick={() => handleSnackbarOpen("Export Filtered data downloaded")}
          >
            <div className="export_option">Export Filtered Data</div>
          </CSVLink>
        </div>
        <div className="export_few">
          <CSVLink
            data={filterDataByCurrentView(exportSpecificRows(rowsPerPage))}
            headers={getFilteredHeaders(csvHeaders, currentViewData)}
            filename="current_fields.csv"
            onClick={() => handleSnackbarOpen(`Export ${rowsPerPage} row(s) data downloaded`)}
          >
            <div className="export_option">Export Current Fields<span>({rowsPerPage})</span> Row(s)</div>
          </CSVLink>
        </div>
      </div>

      <DownloadSnackbar open={snackbarOpen} message={snackbarMessage} handleClose={handleSnackbarClose} />
    </div>
  );
};


// Search Component
export const Search = ({onSearch, filterText}) => {
  return (
    <div className='rightside-search'>
    <input className='topHeaderToolBar'
        type="text"
        placeholder="Search within..."
        value={filterText}
        onChange={e => onSearch(e.target.value)}
    />
    <i className="ri-search-line"></i>
  </div>
  )
}
