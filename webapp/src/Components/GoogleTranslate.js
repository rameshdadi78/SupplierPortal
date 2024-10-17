// GoogleTranslate.js
import React, { useEffect } from 'react';

const GoogleTranslate = () => {
  const googleTranslateElementInit = () => {
    try {
      new window.google.translate.TranslateElement(
        {
          pageLanguage: "en",
          autoDisplay: false,
        },
        "google_translate_element"
      );
    } catch (error) {
      console.error("Error initializing Google Translate:", error);
    }
  };
  

  const clearGoogleTranslateCookies = () => {
    document.cookie
      .split(";")
      .forEach(function (c) {
        document.cookie = c
          .replace(/^ +/, "")
          .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
      });
  };

  useEffect(() => {
    clearGoogleTranslateCookies(); 

    const addScript = document.createElement("script");
    addScript.setAttribute(
      "src",
      "//translate.google.com/translate_a/element.js?cb=googleTranslateElementInit"
    );
    document.body.appendChild(addScript);
    window.googleTranslateElementInit = googleTranslateElementInit;
  }, []);

  return <div id="google_translate_element" style={{ margin: '10px 0' }}></div>;
};

export default GoogleTranslate;
