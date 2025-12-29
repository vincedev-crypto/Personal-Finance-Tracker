// transactions.js

document.addEventListener('DOMContentLoaded', function () {
    // --- Form and Link References ---
    const sortForm = document.getElementById('sortForm');
    const filterForm = document.getElementById('filterForm');
    const sortLinks = document.querySelectorAll('.sort-link');
    const clearFiltersLink = document.getElementById('clearFiltersLink');

    // --- Event Listener for Sort Links ---
    if (sortLinks && sortForm && filterForm) {
        sortLinks.forEach(link => {
            link.addEventListener('click', function (event) {
                event.preventDefault(); // Prevent default <a> tag navigation

                const sortField = this.dataset.sortfield;
                const sortOrder = this.dataset.sortorder;

                // Populate hidden sort form with sort parameters
                if(document.getElementById('sortSortField')) document.getElementById('sortSortField').value = sortField;
                if(document.getElementById('sortSortOrder')) document.getElementById('sortSortOrder').value = sortOrder;

                // Populate hidden sort form with current filter values from the visible filter form
                if (document.getElementById('sortKeyword') && filterForm.elements.keyword) document.getElementById('sortKeyword').value = filterForm.elements.keyword.value;
                if (document.getElementById('sortType') && filterForm.elements.type) document.getElementById('sortType').value = filterForm.elements.type.value;
                if (document.getElementById('sortCategory') && filterForm.elements.category) document.getElementById('sortCategory').value = filterForm.elements.category.value;
                if (document.getElementById('sortMonth') && filterForm.elements.month) document.getElementById('sortMonth').value = filterForm.elements.month.value;
                if (document.getElementById('sortStartDate') && filterForm.elements.startDate) document.getElementById('sortStartDate').value = filterForm.elements.startDate.value;
                if (document.getElementById('sortEndDate') && filterForm.elements.endDate) document.getElementById('sortEndDate').value = filterForm.elements.endDate.value;
                if (document.getElementById('sortMinAmount') && filterForm.elements.minAmount) document.getElementById('sortMinAmount').value = filterForm.elements.minAmount.value;
                if (document.getElementById('sortMaxAmount') && filterForm.elements.maxAmount) document.getElementById('sortMaxAmount').value = filterForm.elements.maxAmount.value;
                
                console.log("Submitting sortForm with sortField:", sortField, "sortOrder:", sortOrder);
                sortForm.submit();
            });
        });
    } else {
        if (!sortLinks.length) console.log("Sort links not found or empty."); // Changed to .length for NodeList
        if (!sortForm) console.error("Sort form ('sortForm') not found.");
        if (!filterForm) console.error("Filter form ('filterForm') not found.");
    }

    // --- Event Listener for Clear Filters Link ---
    if (clearFiltersLink && filterForm) {
        clearFiltersLink.addEventListener('click', function(event) {
            event.preventDefault();
            
            // Clear visible filter form fields
            if(filterForm.elements.keyword) filterForm.elements.keyword.value = '';
            if(filterForm.elements.type) filterForm.elements.type.value = '';
            if(filterForm.elements.category) filterForm.elements.category.value = '';
            if(filterForm.elements.month) filterForm.elements.month.value = ''; 
            if(filterForm.elements.startDate) filterForm.elements.startDate.value = '';
            if(filterForm.elements.endDate) filterForm.elements.endDate.value = '';
            if(filterForm.elements.minAmount) filterForm.elements.minAmount.value = '';
            if(filterForm.elements.maxAmount) filterForm.elements.maxAmount.value = '';
            
            // Set default sort order in the main filter form for when it's submitted after clearing
            if(filterForm.elements.sortField) filterForm.elements.sortField.value = 'transactionDate'; 
            if(filterForm.elements.sortOrder) filterForm.elements.sortOrder.value = 'DESC';          

            console.log("Submitting filterForm after clearing filters.");
            filterForm.submit(); 
        });
    } else {
        if (!clearFiltersLink) console.log("Clear filters link ('clearFiltersLink') not found.");
        // filterForm check is already part of the previous block
    }

    // --- Calculate and Display Total Amount ---
    // This is the code you provided
    const amountCells = document.querySelectorAll("td.amount"); // Make sure your amount cells have class="amount"
    let total = 0;

    amountCells.forEach(cell => {
        // Assuming the text content is like "1,234.56" or "₱1,234.56"
        // We need to remove currency symbols and commas before parsing
        const rawValue = cell.textContent.replace(/[₱,]/g, '').trim();
        const value = parseFloat(rawValue);
        if (!isNaN(value)) {
            total += value;
        } else {
            console.warn("Could not parse amount from cell content:", cell.textContent);
        }
    });

    const totalAmountElement = document.getElementById("total-amount");
    if (totalAmountElement) {
        // The controller already sends a formatted totalAmount.
        // If you want to calculate it client-side, this is how you'd format it:
        // const formattedTotal = new Intl.NumberFormat('en-PH', {
        //     style: 'currency',
        //     currency: 'PHP'
        // }).format(total);
        // totalAmountElement.innerHTML = `<strong>${formattedTotal}</strong>`;
        
        // If your controller already provides `totalAmount` formatted or unformatted,
        // and it's already in the `total-amount` element's `<strong>` tag via Thymeleaf,
        // then this client-side calculation might be redundant or for display updates
        // if the table changes dynamically without a page reload.
        // For now, let's assume the Thymeleaf part handles the initial display.
        // If you want this JS to *override* the Thymeleaf value, uncomment the lines above.
        console.log("Client-side calculated total (raw):", total);
        // If you want to update the display with the client-side calculated total:
        // const formattedTotal = new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(total);
        // totalAmountElement.innerHTML = `<strong>${formattedTotal}</strong>`;
    } else {
        console.log("Total amount element ('total-amount') not found.");
    }

    console.log("transactions.js loaded and DOM fully processed.");
});
