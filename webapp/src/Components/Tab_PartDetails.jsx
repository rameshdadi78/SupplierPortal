import { useState, useEffect,useContext } from 'react';
import { MainContext } from "./MainSection";
import columnOrderConfig from './columnOrderConfig.json';


export const Tab_PartDetails = ({selectedRow}) => {
    // const selectedRow = useContext(MainContext);
    const [basicAttribute, setBasicAttribute] = useState({});
    const [otherAttribute, setOtherAttribute] = useState({});
    const [isGeneralOpen, setIsGeneralOpen] = useState(true);
    const [isXploriaOpen, setIsXploriaOpen] = useState(true);
    const toggleGeneral = () => setIsGeneralOpen(!isGeneralOpen);
    const toggleXploria = () => setIsXploriaOpen(!isXploriaOpen);


    useEffect(() => {
        if (selectedRow) {
          const result = selectedRow.results[0];
          const partDataKey = Object.keys(result).find(key => key.startsWith("objectId:"));
          // console.log("Part Data Key:", partDataKey);
      
          if (partDataKey) {
            const partData = result[partDataKey];
            // console.log(partData);
      
            if (partData) {
              // Basic Attributes
              const basicAttributeKeys = columnOrderConfig.PartDetailsSections["Basic Attributes"];
              const basicAttributes = basicAttributeKeys.reduce((acc, attr) => {
                  const matchingAttribute = partData.basicAttributes.find(item => item.displayName === attr);
                  if (matchingAttribute) {
                      acc[matchingAttribute.displayName] = matchingAttribute.value;
                  } else {
                      acc[attr] = "N/A";  // Default if no matching attribute
                  }
                  return acc;
              }, {});
              setBasicAttribute(basicAttributes);
              



               // Other Attributes
               const otherAttributeKeys = columnOrderConfig.PartDetailsSections["Other Attributes"];
               const otherAttributes = otherAttributeKeys.reduce((acc, attr) => {
                   const matchingAttribute = partData.attributes.find(item => item.displayName === attr);
                   if (matchingAttribute) {
                       acc[matchingAttribute.displayName] = matchingAttribute.value;
                   } else {
                       acc[attr] = "N/A";  // Default if no matching attribute
                   }
                   return acc;
               }, {});
              setOtherAttribute(otherAttributes);
            }
          }
        }
      }, [selectedRow]);
  return (
    <>
     <div className='tab_container'>
          <div className='tab-header' onClick={toggleGeneral}>
            <h3 className='tab-header-title'>Basic Attributes</h3>
            <i className={`ri-arrow-down-double-line ${isGeneralOpen ? 'rotate_icon' : ''}`}></i>
          </div>
          <div className={`part-details-grid ${isGeneralOpen ? 'open' : ''}`}>
            {basicAttribute && Object.keys(basicAttribute).map(key => (
              <p className="part-details-item" key={key}>
                {key}: <span>{basicAttribute[key]}</span>
              </p>
            ))}
          </div>

          <div className='tab-header' onClick={toggleXploria}>
            <h3 className='tab-header-title'>Other Attributes</h3>
            <i className={`ri-arrow-down-double-line ${isXploriaOpen ? 'rotate_icon' : ''}`}></i>
          </div>
          <div className={`part-details-grid ${isXploriaOpen ? 'open' : ''}`}>
            {otherAttribute && Object.keys(otherAttribute).map(key => (
              <p className="part-details-item" key={key}>
                {key}: <span>{otherAttribute[key]}</span>
              </p>
            ))}
          </div>
        </div>
    </>
  )
}
