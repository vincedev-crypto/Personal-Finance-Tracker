(function() {
    const INACTIVITY_TIMEOUT = 5 * 60 * 1000; // 5 minutes in milliseconds
    let inactivityTimer;

    // Retrieve the logout URL passed from Thymeleaf
    // This requires the calling HTML to set a global variable `logoutUrl`
    const logoutRedirectUrl = typeof window.logoutUrl !== 'undefined' ? window.logoutUrl : '/logout';

    function resetTimer() {
        clearTimeout(inactivityTimer);
        inactivityTimer = setTimeout(logoutUser, INACTIVITY_TIMEOUT);
    }

    function logoutUser() {
        console.log("User inactive for 5 minutes. Logging out...");
        window.location.href = logoutRedirectUrl;
    }

    // Events that reset the timer
    window.onload = resetTimer;
    document.onmousemove = resetTimer;
    document.onkeypress = resetTimer;
    document.onclick = resetTimer;
    document.onscroll = resetTimer;
    document.ontouchstart = resetTimer; // For touch devices

    // Initial call to start the timer
    resetTimer(); 
    console.log("Inactivity timer started for 5 minutes. Logout URL: " + logoutRedirectUrl);
})();