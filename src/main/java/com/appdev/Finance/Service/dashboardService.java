package com.appdev.Finance.Service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class dashboardService {
		
	
	 public static Map<String, Object> getDashboardData() {
	        Map<String, Object> dashboardData = new HashMap<>();
	        dashboardData.put("totalIncome", "No data");
	        dashboardData.put("totalExpenses", "No data");
	        dashboardData.put("budgetUsage", "No budget set");
	        dashboardData.put("savings", "No data");
	        dashboardData.put("recentTransactions", "No recent transactions");
	        return dashboardData;
	    }

	    public static Map<String, Object> setBudget(Map<String, Object> budgetData) {
	        // Save budget data to database (mock response for now)
	        return Map.of("message", "Budget set successfully", "budget", budgetData);
	    }
}
