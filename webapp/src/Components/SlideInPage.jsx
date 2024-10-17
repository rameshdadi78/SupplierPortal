import { useRef,useEffect,useState,useContext} from "react";
import pdfdownload_Icon from "../Icons/pdfdownload.png";
import propcloseIcon from '../Icons/PropCloseIcon.png';
import jsPDF from 'jspdf';
import Pdf_log from '../Icons/xp_logo.png';
import DataTable from 'react-data-table-component';
import { MainContext } from "./MainSection";

export const SlideInPage = ({ data, onClose, currentTab, fileName, selectedSections,isSlideInOpen }) => {
  const selectedRow = useContext(MainContext);

const slideInRef = useRef(null);
const userName = localStorage.getItem('username');


const [partTableData, setPartTableData] = useState([]);
  const [basicAttribute, setBasicAttribute] = useState(null); // Corrected setter
  const [otherAttribute, setOtherAttribute] = useState(null); // Corrected setter
  const [selectedTab, setSelectedTab] = useState(0); // Add state for selectedTab

  useEffect(() => {
    if (selectedRow && selectedRow.results && selectedRow.results.length > 0) {
      const result = selectedRow.results[0]; // Fetch the first result

      // Assuming the objectId is part of the key
      const objectIdKey = Object.keys(result).find(key => key.startsWith('objectId:'));
      if (!objectIdKey) {
        console.error('ObjectId not found');
        return;
      }

      const partData = result[objectIdKey]; // Accessing the data using the objectId

      if (partData) {
        // Extract basic attributes
        const basicAttrs = partData.basicAttributes.reduce((acc, attr) => {
          acc[attr.displayName] = attr.value;
          return acc;
        }, {});
        setBasicAttribute(basicAttrs); // Corrected setter

        // Extract other attributes
        const otherAttrs = partData.attributes.reduce((acc, attr) => {
          acc[attr.displayName] = attr.value;
          return acc;
        }, {});
        setOtherAttribute(otherAttrs); // Corrected setter

        // Format the data for display in the part table
        const formattedPartData = [{
          type: basicAttrs.Type || otherAttrs.Type || 'N/A',
          name: basicAttrs.Name || otherAttrs.Name || 'N/A',
          revision: basicAttrs.Revision || otherAttrs.Revision || 'N/A',
          description: basicAttrs.Description || otherAttrs.Description || 'N/A',
          state: basicAttrs.State || otherAttrs.State || 'N/A',
        }];

        if (formattedPartData.length > 0) {
          setPartTableData(formattedPartData); // Corrected setter
        } else {
          console.error('No Part Details found');
        }

        // Set the default selected tab
        setSelectedTab(0); // Corrected setter

        // Merge basic and other attributes
        const parentAttributes = { ...basicAttrs, ...otherAttrs, objectId: objectIdKey.split(': ')[1] };
        console.log(parentAttributes);
      } else {
        console.error('No part data found for the given objectId.');
      }
    }
  }, [selectedRow]);






    // // Helper function to format field labels
    const formatLabel = (label) => {
      return label
        .replace(/([A-Z])/g, ' $1') // Add space before capital letters
        .replace(/^./, (str) => str.toUpperCase()); // Capitalize the first letter
    };
  
    // // Helper function to format date fields to remove time stamp
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleDateString();
    };
  
    // // Check if the field is a date
    const isDateField = (field) => field.toLowerCase().includes('date');
  
    // // Close slide-in when clicking outside of it
    useEffect(() => {
      const handleClickOutside = (event) => {
        if (slideInRef.current && !slideInRef.current.contains(event.target)) {
          onClose();
        }
      };
  
      document.addEventListener('mousedown', handleClickOutside);
      return () => {
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }, [onClose]);
  
    // // Helper function to render a row of four fields
    const renderFieldRow = (fields) => (
      <div className="row">
        {fields.map((field, index) => (
          <div key={index} className="field">
            <span className="label">{formatLabel(field)}:</span>
            <span className="value">
              {isDateField(field) ? formatDate(data?.[field]) : data?.[field] || '-'}
            </span>
          </div>
        ))}
      </div>
    );
  
    const columns = [
      { name: 'Name', selector: row => row.name, sortable: true },
      { name: 'Type', selector: row => row.type, sortable: true },
      { name: 'Revision', selector: row => row.revision, sortable: true },
      { name: 'Description', selector: row => row.description, sortable: true },
      { name: 'State', selector: row => row.state, sortable: true },
    ];
    // // Split fields into groups of four
    const getGroupedFields = (fields) => {
      const groups = [];
      for (let i = 0; i < fields.length; i += 2) {
        groups.push(fields.slice(i, i + 2));
      }
      return groups;
    };
    const generatePDF = async (data, sections, filename, userName) => {
        const pdf = new jsPDF('p', 'mm', 'a4');
        const pageHeight = pdf.internal.pageSize.height;
        const pageWidth = pdf.internal.pageSize.width;
        let yPosition = 30; // Adjusted yPosition to make space for the header
      
        // Function to add the header and footer
        const addHeaderFooter = () => {
          const currentPage = pdf.internal.getCurrentPageInfo().pageNumber;
         /* const pageCount = pdf.internal.getNumberOfPages(); */
          // const timestamp = new Date().toLocaleString();
          // const timestamp = new Date().toLocaleString('en-GB', { timeZone: 'GMT', timeZoneName: 'short' });
          const timestamp = new Date().toUTCString();
      
      
          // Add logo to the header only on the first page
          // if (currentPage === 1) {
          //   pdf.addImage(xp_log, 'PNG', 10, 5, 18, 15); 
          // } 
      
          // Add user name and timestamp to the header on the right side
          pdf.addImage(Pdf_log, 'PNG', 10, 5, 18, 15);
          pdf.setFontSize(10);
          pdf.text(`user: ${userName}`, pageWidth - 80, 10, null, null, 'left'); 
          pdf.text(`time: ${timestamp}`, pageWidth - 80, 15, null, null, 'left'); 
      
          // Header border below
          pdf.setDrawColor(0, 0, 0); // Black color for the border
          pdf.setLineWidth(0.5); // Border thickness
          pdf.line(10, 22, pageWidth - 10, 22); // Draw line below the header
      
          // Footer 
          pdf.line(10, pageHeight - 20, pageWidth - 10, pageHeight - 20); // Draw line above the footer
      
          // Footer content
          pdf.setFontSize(10);
          pdf.text(`${currentPage}`, 105, pageHeight - 10, null, null, 'center');
          // pdf.text(`User: ${userName}`, 10, pageHeight - 10); 
          // pdf.text(`Timestamp: ${timestamp}`, 10, pageHeight - 5); 
        };
      
        // Function to check page breaks and avoid footer overlap
        const checkPageBreak = (yPosition) => {
          if (yPosition > pageHeight - 30) { // Adjusted for footer space
            pdf.addPage();
            yPosition = 30; // Reset yPosition after adding a new page
            addHeaderFooter(); // Add header and footer to the new page
          }
          return yPosition;
        };
      
        // First page header and footer
        addHeaderFooter(); // Make sure it's added before content starts
      
        // Add PDF content
        Object.keys(sections).forEach((sectionTitle) => {
          yPosition = checkPageBreak(yPosition);
      
          pdf.setFillColor(230, 230, 250); // Light lavender background color
          pdf.rect(10, yPosition, 190, 10, 'F'); // Fills the rectangle area
          pdf.setTextColor(0, 0, 0); // Black color for text
          pdf.setFontSize(16);
          pdf.text(sectionTitle, 15, yPosition + 7); // Slightly inside the rectangle
          yPosition += 15; // Space after section header
      
          const groupedFields = getGroupedFields(sections[sectionTitle].filter(field => field !== 'Title' && field !== 'Description'));
      
          groupedFields.forEach((group) => {
            let xPosition = 10; // Initial horizontal position for the group
      
            group.forEach((field) => {
              yPosition = checkPageBreak(yPosition);
      
              pdf.setFillColor(255, 255, 255); // White background
              pdf.rect(xPosition, yPosition, 90, 8, 'F'); // Fills the rectangle area
              pdf.setFontSize(10); // Add field label with adjusted spacing
              pdf.setTextColor(50, 50, 50); // Dark gray for label
              const fieldText = `${field}  :`;
              pdf.text(fieldText, xPosition + 2, yPosition + 6); 
      
              pdf.setFont('helvetica', 'normal');
              pdf.setTextColor(0, 0, 0); // Black for value
      
              const valueText = data[field] || '-';
      
              // Check if the value text exceeds the maximum length
              if (valueText.length > 40) {
                const splitValueText = pdf.splitTextToSize(valueText, 90 - 50 + 2);
                pdf.text(splitValueText, xPosition + 55, yPosition + 6);
                yPosition += 8 * (splitValueText.length - 1); // Adjust yPosition based on the number of lines used for the value
              } else {
                pdf.text(valueText, xPosition + 55, yPosition + 6); // Align value at fixed position
              }
      
              xPosition += 90 + 5; // Move xPosition to the right for the next group
            });
      
            yPosition += 10;
          });
      
         
          if (sections[sectionTitle].includes('Title')) {
            yPosition = checkPageBreak(yPosition);
      
            pdf.setFillColor(255, 255, 255); // White background
            pdf.rect(10, yPosition, 190, 8, 'F'); // Fills the rectangle area
            pdf.setFontSize(10);
            pdf.setTextColor(100, 100, 100); // Gray color for label
            pdf.text('Title  :', 12, yPosition + 6);
            pdf.setFont('helvetica', 'normal');
            pdf.setTextColor(0, 0, 0); // Black color for value
            const titleText = data?.Title || '-';
            if (titleText.length > 40) {
              const splitTitleText = pdf.splitTextToSize(titleText, 190 - 62 - 2);
              pdf.text(splitTitleText, 62, yPosition + 6);
              yPosition += 8 * (splitTitleText.length - 1);
            } else {
              pdf.text(titleText, 62, yPosition + 6);
            }
            yPosition += 8;
          }
      
          if (sections[sectionTitle].includes('Description')) {
            yPosition = checkPageBreak(yPosition);
      
            pdf.setFillColor(255, 255, 255); // White background
            pdf.rect(10, yPosition, 190, 8, 'F'); // Fills the rectangle area
            pdf.setFontSize(10);
            pdf.setTextColor(100, 100, 100); // Gray color for label
            pdf.text('Description  :', 12, yPosition + 6);
            pdf.setFont('helvetica', 'normal');
            pdf.setTextColor(0, 0, 0); // Black color for value
            const descriptionText = data?.Description || '-';
            if (descriptionText.length > 40) {
              const splitDescriptionText = pdf.splitTextToSize(descriptionText, 190 - 62 - 2);
              pdf.text(splitDescriptionText, 62, yPosition + 6);
              yPosition += 8 * (splitDescriptionText.length - 1);
            } else {
              pdf.text(descriptionText, 62, yPosition + 6);
            }
            yPosition += 8;
          }
      
          yPosition += 10; // Space before the next section
        });
      
      
        pdf.save(filename);
      };
      const downloadPDF = () => {
        generatePDF(data, selectedSections, fileName,userName);
      };
  


  
          return (
            <div ref={slideInRef} className={`slide-in-page ${isSlideInOpen ? 'open' : ''}`}>
            <div className="header">
              {/* <p>999999999999999999999999999999999999999999999999999999</p> */}
              <button className="pdfbutton" onClick={downloadPDF}>
                <img src={pdfdownload_Icon} alt="Download PDF" />
              </button>
              <button onClick={onClose} className="closebutton">
                <img src={propcloseIcon} alt="Close" />
              </button> 
            </div>
          
            <div className="content">
              {Object.keys(selectedSections).map((sectionTitle) => (
                <div key={sectionTitle} className="section">
                  <h3>{sectionTitle}</h3>
                  <div className="fields">
                    {selectedSections[sectionTitle]
                      .filter((field) => field !== 'Title' && field !== 'Description')
                      .length > 0 &&
                      getGroupedFields(
                        selectedSections[sectionTitle].filter(
                          (field) => field !== 'Title' && field !== 'Description'
                        )
                      ).map((group, index) => (
                        <div key={index} className="field-group">
                          {renderFieldRow(group)}
                        </div>
                      ))}
          
                    {selectedSections[sectionTitle].includes('Title') && (
                      <div className="row full-width">
                        <span className="label">{formatLabel('Title')}:</span>
                        <span className="value">{data?.Title || '-'}</span>
                      </div>
                    )}
                    {selectedSections[sectionTitle].includes('Description') && (
                      <div className="row full-width">
                        <span className="label">{formatLabel('Description')}:</span>
                        <span className="value">{data?.Description || '-'}</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
          
              {currentTab === 'Deviation' && (
                <div className="section">
                  <h3>AffectedItems</h3>
                  <DataTable
                    columns={columns}
                    data={partTableData} // Assuming you have only one part detail
                    pagination
                    paginationPerPage={5}
                    paginationRowsPerPageOptions={[5, 10]}
                    highlightOnHover
                    responsive
                  />
                </div>
              )}
            </div>
          </div>
          
          );
    
  };
  