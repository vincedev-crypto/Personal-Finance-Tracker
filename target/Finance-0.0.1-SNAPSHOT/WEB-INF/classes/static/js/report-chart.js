// --- report-chart.js ---

google.charts.load('current', { packages: ['corechart'] });
google.charts.setOnLoadCallback(drawReportCharts); // Renamed function for clarity (optional)

function drawReportCharts() { // Renamed function for clarity (optional)
    try {
        // These variables are defined in the inline script in report.html
        const parsedCategoryData = JSON.parse(categoryChartData);
        const parsedMonthlyData = JSON.parse(monthlyChartData);

        // --- Spending Analysis Chart ---
        const categoryDataArray = [['Category', 'Amount']];
        if (Object.keys(parsedCategoryData).length > 0) {
            for (const category in parsedCategoryData) {
                if (Object.hasOwnProperty.call(parsedCategoryData, category)) {
                    categoryDataArray.push([category, parsedCategoryData[category]]);
                }
            }
        } else {
             // Optional: Handle no data case if needed
             // categoryDataArray.push(['No Spending Data', 0]);
        }

        const spendingData = google.visualization.arrayToDataTable(categoryDataArray);

        const spendingOptions = {
            // Title can be dynamic based on selectedMonth if passed from controller
            title: 'Spending Analysis by Category', // Adjust title if category data is filtered by month
            pieHole: 0.4,
            chartArea: { width: '90%', height: '80%' },
            legend: { position: 'bottom' }
            // Add other options as needed
        };

        // Use the ID from report.html
        const categoryChartDiv = document.getElementById('reportSpendingByCategoryChart');
        if (categoryChartDiv) {
            const spendingChart = new google.visualization.PieChart(categoryChartDiv);
            spendingChart.draw(spendingData, spendingOptions);
        } else {
            console.error("Chart container div with ID 'reportSpendingByCategoryChart' not found.");
        }

        // --- Monthly Expenses Chart ---
        const months = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];

        const monthlyDataArray = [['Month', 'Expenses']];
        if (Object.keys(parsedMonthlyData).length > 0) {
            months.forEach(month => {
                monthlyDataArray.push([month, parsedMonthlyData[month] || 0]);
            });
        } else {
            // Optional: Handle no data case if needed
            // months.forEach(month => { monthlyDataArray.push([month, 0]); });
        }


        const monthlyData = google.visualization.arrayToDataTable(monthlyDataArray);

        const monthlyOptions = {
            title: 'Monthly Expenses (All Months)', // Title reflects all months data
            curveType: 'function',
            legend: { position: 'bottom' },
            vAxis: { format: '₱#,##0.00', title: 'Amount (PHP)' }, // Use PHP currency symbol
             hAxis: { title: 'Month' }
            // Add other options as needed
        };

        // Use the ID from report.html
        const monthlyChartDiv = document.getElementById('reportExpensesByMonthChart');
        if (monthlyChartDiv) {
            const monthlyChart = new google.visualization.LineChart(monthlyChartDiv);
            monthlyChart.draw(monthlyData, monthlyOptions);
        } else {
            console.error("Chart container div with ID 'reportExpensesByMonthChart' not found.");
        }

    } catch (e) {
        console.error("Error drawing report charts:", e);
        // Optionally display a user-friendly message on the page in case of errors
        // Example: if(categoryChartDiv) categoryChartDiv.innerHTML = "Error loading category chart.";
        // Example: if(monthlyChartDiv) monthlyChartDiv.innerHTML = "Error loading monthly chart.";
    }
}