// dashboard-chart.js

// --- Google Charts Logic ---
google.charts.load('current', { packages: ['corechart', 'line'] });
google.charts.setOnLoadCallback(drawDashboardCharts);

function drawDashboardCharts() {
    try {
        // Spending by Category Chart (Pie Chart)
        if (typeof categoryChartData !== 'undefined' && categoryChartData) {
            const parsedCategoryData = JSON.parse(categoryChartData);
            const categoryDataArray = [['Category', 'Amount']];
            if (Object.keys(parsedCategoryData).length > 0) {
                for (const category in parsedCategoryData) {
                    if (Object.hasOwnProperty.call(parsedCategoryData, category)) {
                        categoryDataArray.push([category, parsedCategoryData[category]]);
                    }
                }
            } else {
                categoryDataArray.push(['No Spending Data', 0]); // Placeholder for no data
            }
            const spendingData = google.visualization.arrayToDataTable(categoryDataArray);
            const spendingOptions = {
                title: 'Spending Analysis by Category (All Time)',
                pieHole: 0.4,
                backgroundColor: 'transparent', // Match dark theme
                legendTextStyle: { color: '#adb5bd' },
                titleTextStyle: { color: '#dee2e6' },
                chartArea: { width: '90%', height: '80%' },
                colors: ['#17a2b8', '#28a745', '#ffc107', '#dc3545', '#6f42c1', '#fd7e14', '#20c997'] // Custom colors
            };
            const spendingChartDiv = document.getElementById('spendingByCategoryChart');
            if (spendingChartDiv) {
                const spendingChart = new google.visualization.PieChart(spendingChartDiv);
                spendingChart.draw(spendingData, spendingOptions);
            } else {
                console.warn("Chart container 'spendingByCategoryChart' not found.");
            }
        } else {
            console.warn("categoryChartData is not defined or empty for dashboard.");
            const spendingChartDiv = document.getElementById('spendingByCategoryChart');
            if(spendingChartDiv) spendingChartDiv.innerHTML = '<p class="text-center text-muted">Category spending data not available.</p>';
        }

        // Expenses by Month Chart (Line Chart)
        if (typeof monthlyChartData !== 'undefined' && monthlyChartData) {
            const parsedMonthlyData = JSON.parse(monthlyChartData);
            const monthsOrder = [
                'January', 'February', 'March', 'April', 'May', 'June',
                'July', 'August', 'September', 'October', 'November', 'December'
            ];
            const monthlyDataArray = [['Month', 'Expenses']];

            if (Object.keys(parsedMonthlyData).length > 0) {
                monthsOrder.forEach(month => {
                    monthlyDataArray.push([month, parsedMonthlyData[month] || 0]);
                });
            } else {
                 monthsOrder.forEach(month => { // Placeholder for no data
                    monthlyDataArray.push([month, 0]);
                });
            }
            const expensesData = google.visualization.arrayToDataTable(monthlyDataArray);
            const expensesOptions = {
                title: 'Expenses by Month (All Time)',
                curveType: 'function',
                legend: { position: 'bottom', textStyle: { color: '#adb5bd' } },
                backgroundColor: 'transparent',
                titleTextStyle: { color: '#dee2e6' },
                hAxis: { textStyle: { color: '#adb5bd' }, titleTextStyle: { color: '#adb5bd' }, title: 'Month' },
                vAxis: { textStyle: { color: '#adb5bd' }, titleTextStyle: { color: '#adb5bd' }, format: '₱#,##0.00', title: 'Amount (PHP)' },
                colors: ['#28a745'] // Custom color for expenses line
            };
            const monthlyChartDiv = document.getElementById('expensesByMonthChart');
            if (monthlyChartDiv) {
                const expensesChart = new google.visualization.LineChart(monthlyChartDiv);
                expensesChart.draw(expensesData, expensesOptions);
            } else {
                console.warn("Chart container 'expensesByMonthChart' not found.");
            }
        } else {
            console.warn("monthlyChartData is not defined or empty for dashboard.");
            const monthlyChartDiv = document.getElementById('expensesByMonthChart');
            if(monthlyChartDiv) monthlyChartDiv.innerHTML = '<p class="text-center text-muted">Monthly expenses data not available.</p>';
        }
    } catch (e) {
        console.error("Error drawing dashboard charts:", e);
        const spendingChartDiv = document.getElementById('spendingByCategoryChart');
        if(spendingChartDiv) spendingChartDiv.innerHTML = '<p class="text-center text-danger">Error loading category chart.</p>';
        const monthlyChartDiv = document.getElementById('expensesByMonthChart');
        if(monthlyChartDiv) monthlyChartDiv.innerHTML = '<p class="text-center text-danger">Error loading monthly chart.</p>';
    }
}


// --- DOM Interaction Logic (Sorting, Filtering, Receipt Modal) ---
document.addEventListener('DOMContentLoaded', function () {
    console.log("Dashboard DOM fully loaded. Initializing interactions...");

    const dashboardSortForm = document.getElementById('dashboardSortForm');
    const dashboardFilterForm = document.getElementById('dashboardFilterForm');
    const dashboardSortLinks = document.querySelectorAll('.dashboard-sort-link');
    const dashboardClearFiltersLink = document.getElementById('dashboardClearFiltersLink');
    const receiptModalElement = document.getElementById('receiptModal'); // Renamed to avoid conflict

    // --- POST-based Filtering and Sorting on Dashboard ---
    if (dashboardSortLinks.length > 0 && dashboardSortForm && dashboardFilterForm) {
        dashboardSortLinks.forEach(link => {
            link.addEventListener('click', function (event) {
                event.preventDefault(); 

                const sortField = this.dataset.sortfield;
                const sortOrder = this.dataset.sortorder;

                if(document.getElementById('dashboardSortSortField')) document.getElementById('dashboardSortSortField').value = sortField;
                if(document.getElementById('dashboardSortSortOrder')) document.getElementById('dashboardSortSortOrder').value = sortOrder;

                // Populate hidden sort form with current filter values
                if(document.getElementById('dashboardSortSelectedMonth') && dashboardFilterForm.elements.selectedMonth) document.getElementById('dashboardSortSelectedMonth').value = dashboardFilterForm.elements.selectedMonth.value;
                if(document.getElementById('dashboardSortKeyword') && dashboardFilterForm.elements.keyword) document.getElementById('dashboardSortKeyword').value = dashboardFilterForm.elements.keyword.value;
                if(document.getElementById('dashboardSortType') && dashboardFilterForm.elements.type) document.getElementById('dashboardSortType').value = dashboardFilterForm.elements.type.value;
                if(document.getElementById('dashboardSortCategory') && dashboardFilterForm.elements.category) document.getElementById('dashboardSortCategory').value = dashboardFilterForm.elements.category.value;
                if(document.getElementById('dashboardSortStartDate') && dashboardFilterForm.elements.startDate) document.getElementById('dashboardSortStartDate').value = dashboardFilterForm.elements.startDate.value;
                if(document.getElementById('dashboardSortEndDate') && dashboardFilterForm.elements.endDate) document.getElementById('dashboardSortEndDate').value = dashboardFilterForm.elements.endDate.value;
                if(document.getElementById('dashboardSortMinAmount') && dashboardFilterForm.elements.minAmount) document.getElementById('dashboardSortMinAmount').value = dashboardFilterForm.elements.minAmount.value;
                if(document.getElementById('dashboardSortMaxAmount') && dashboardFilterForm.elements.maxAmount) document.getElementById('dashboardSortMaxAmount').value = dashboardFilterForm.elements.maxAmount.value;
                
                console.log("Submitting dashboardSortForm for sorting. Field:", sortField, "Order:", sortOrder);
                dashboardSortForm.submit();
            });
        });
    } else {
        if (!dashboardSortLinks.length) console.log("Dashboard sort links (.dashboard-sort-link) not found.");
        if (!dashboardSortForm) console.error("Dashboard sort form ('dashboardSortForm') not found.");
        if (!dashboardFilterForm) console.error("Dashboard filter form ('dashboardFilterForm') not found.");
    }

    if (dashboardClearFiltersLink && dashboardFilterForm) {
        dashboardClearFiltersLink.addEventListener('click', function(event) {
            event.preventDefault();
            
            // Clear visible filter form fields
            // For selectedMonth, it's better to let the controller set the default (current month) on POST if empty
            if(dashboardFilterForm.elements.selectedMonth) dashboardFilterForm.elements.selectedMonth.value = ''; 
            if(dashboardFilterForm.elements.keyword) dashboardFilterForm.elements.keyword.value = '';
            if(dashboardFilterForm.elements.type) dashboardFilterForm.elements.type.value = '';
            if(dashboardFilterForm.elements.category) dashboardFilterForm.elements.category.value = '';
            if(dashboardFilterForm.elements.startDate) dashboardFilterForm.elements.startDate.value = '';
            if(dashboardFilterForm.elements.endDate) dashboardFilterForm.elements.endDate.value = '';
            if(dashboardFilterForm.elements.minAmount) dashboardFilterForm.elements.minAmount.value = '';
            if(dashboardFilterForm.elements.maxAmount) dashboardFilterForm.elements.maxAmount.value = '';
            
            // Set default sort order in the main filter form's hidden fields
            if(dashboardFilterForm.elements.sortField) dashboardFilterForm.elements.sortField.value = 'transactionDate'; 
            if(dashboardFilterForm.elements.sortOrder) dashboardFilterForm.elements.sortOrder.value = 'DESC';          

            console.log("Submitting dashboardFilterForm after clearing filters.");
            dashboardFilterForm.submit(); 
        });
    } else {
        if (!dashboardClearFiltersLink) console.log("Dashboard clear filters link ('dashboardClearFiltersLink') not found.");
    }

    // --- JavaScript for Receipt Modal on Dashboard ---
    if (receiptModalElement) {
        receiptModalElement.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget; 
            if (!button || !button.matches('.view-receipt-btn')) { // Ensure the event is from our button
                console.log("Modal event not triggered by a .view-receipt-btn");
                return;
            }

            const fileUrl = button.getAttribute('data-file-url');
            const fileName = button.getAttribute('data-file-name');
            const fileType = button.getAttribute('data-file-type');

            const modalTitle = receiptModalElement.querySelector('.modal-title');
            const modalBody = receiptModalElement.querySelector('.modal-body');

            modalTitle.textContent = 'Viewing: ' + (fileName || 'Receipt');
            modalBody.innerHTML = '<p class="text-center py-5"><i class="fas fa-spinner fa-spin fa-2x"></i><br>Loading receipt...</p>'; 

            if (fileUrl) {
                fetch(fileUrl)
                    .then(response => {
                        if (!response.ok) {
                            throw new Error(`HTTP error! Status: ${response.status}`);
                        }
                        const contentTypeFromServer = response.headers.get("content-type");
                        // Prefer server's content type if available, otherwise use data-attribute
                        const effectiveFileType = contentTypeFromServer || fileType; 
                        console.log("Effective file type for display:", effectiveFileType);

                        if (effectiveFileType && effectiveFileType.startsWith('image/')) {
                            return response.blob().then(blob => ({ blob, displayType: 'image' }));
                        } else if (effectiveFileType === 'application/pdf') {
                            return response.blob().then(blob => ({ blob, displayType: 'pdf' }));
                        } else if (effectiveFileType && effectiveFileType.startsWith('text/')) {
                            return response.text().then(text => ({ text, displayType: 'text' }));
                        } else {
                            console.warn("Unknown or unsupported file type for direct display:", effectiveFileType);
                            return response.blob().then(blob => ({ blob, displayType: 'download' }));
                        }
                    })
                    .then(data => {
                        modalBody.innerHTML = ''; // Clear loading message
                        if (data.displayType === 'image') {
                            const imageUrl = URL.createObjectURL(data.blob);
                            const img = document.createElement('img');
                            img.src = imageUrl;
                            img.style.maxWidth = '100%';
                            img.style.maxHeight = '70vh'; 
                            img.onload = () => URL.revokeObjectURL(imageUrl); 
                            modalBody.appendChild(img);
                        } else if (data.displayType === 'pdf') {
                            const pdfUrl = URL.createObjectURL(data.blob);
                            const embed = document.createElement('embed');
                            embed.src = pdfUrl;
                            embed.type = 'application/pdf';
                            embed.style.width = '100%';
                            embed.style.height = '75vh'; // Adjusted for better PDF viewing
                            modalBody.appendChild(embed);
                            // Add a download link as a fallback for PDF issues
                            const downloadLink = document.createElement('a');
                            downloadLink.href = pdfUrl;
                            downloadLink.textContent = 'Download PDF if not displaying correctly';
                            downloadLink.className = 'btn btn-sm btn-outline-secondary mt-2 d-block text-center';
                            downloadLink.download = fileName || 'receipt.pdf';
                            modalBody.appendChild(downloadLink);
                        } else if (data.displayType === 'text') {
                            const pre = document.createElement('pre');
                            pre.style.whiteSpace = 'pre-wrap'; 
                            pre.style.wordBreak = 'break-all';
                            pre.textContent = data.text;
                            modalBody.appendChild(pre);
                        } else if (data.displayType === 'download' && data.blob) {
                             modalBody.innerHTML = `<p class="text-center">Cannot display this file type directly.</p>
                                                   <p class="text-center"><a href="${fileUrl}" target="_blank" download="${fileName || 'download'}" class="btn btn-primary">Download File</a></p>`;
                        } else {
                             modalBody.textContent = 'Could not load or display the receipt content.';
                        }
                    })
                    .catch(error => {
                        console.error('Error fetching receipt:', error);
                        modalBody.innerHTML = `<p class="text-center text-danger">Error loading receipt: ${error.message}. Please try again or check the console.</p>`;
                    });
            } else {
                modalBody.textContent = 'Receipt URL not found on the button.';
                console.error("Receipt URL not found on button:", button);
            }
        });
    } else {
        console.warn("Receipt modal element ('receiptModal') not found.");
    }
    console.log("Dashboard interaction JS (sorting/filtering/receipts) fully initialized.");
});
