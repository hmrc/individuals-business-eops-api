<p>Scenario simulations using Gov-Test-Scenario headers is only available in the sandbox environment.</p>
<table>
    <thead>
        <tr>
            <th>Header Value (Gov-Test-Scenario)</th>
            <th>Scenario</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>N/A - DEFAULT</p></td>
            <td><p>Simulate success response.</p></td>
        </tr>
        <tr>
            <td><p>MATCHING_RESOURCE_NOT_FOUND</p></td>
            <td><p>Simulate scenario where no data found.</p></td>
        </tr>
        <tr>
            <td><p>RULE_ALREADY_SUBMITTED</p></td>
            <td><p>Simulate scenario where the user has previously submitted an End of Period Statement for this business' accounting period.</p></td>
        </tr>
        <tr>
            <td><p>RULE_CLASS4_OVER_16</p></td>
            <td><p>Simulate scenario where the user has requested a National Insurance Class 4 exemption but the individual’s age is greater than or equal to 16 years old on the 6th April of the current tax year.</p></td>
        </tr>
        <tr>
            <td><p>RULE_CLASS4_PENSION_AGE</p></td>
            <td><p>Simulate scenario where the user has requested a National Insurance Class 4 exemption but the individual's age is less than their State Pension age on the 6th April of the current tax year.</p></td>
        </tr>
        <tr>
            <td><p>RULE_CONSOLIDATED_EXPENSES_SELF_EMPLOYMENT</p></td>
            <td><p>Simulate scenario where self employment consolidated expenses are not allowed if the cumulative turnover amount exceeds the threshold.</p></td>
        </tr>
        <tr>
            <td><p>RULE_CONSOLIDATED_EXPENSES_UK_PROPERTY</p></td>
            <td><p>Simulate scenario where UK property consolidated expenses are not allowed if the cumulative turnover amount exceeds the threshold.</p></td>
        </tr>
        <tr>
            <td><p>RULE_EARLY_SUBMISSION</p></td>
            <td><p>Simulate scenario where the user has tried to make their End of Period Statement declaration before the accounting period has ended.</p></td>
        </tr>
        <tr>
            <td><p>RULE_FHL_PRIVATE_USE_ADJUSTMENT</p></td>
            <td><p>Simulate scenario for UK Furnished Holiday Lettings, the private use adjustment exceeds the total allowable expenses.</p></td>
        </tr>
        <tr>
            <td><p>RULE_LATE_SUBMISSION</p></td>
            <td><p>Simulate scenario where the user has tried to make their End of Period Statement declaration too late.</p></td>
        </tr>
        <tr>
            <td><p>RULE_MISMATCHED_END_DATE_PERIOD_SHORT</p></td>
            <td><p>Simulate scenario where the period submission end date does not match the accounting period end date.</p></td>
        </tr>
        <tr>
            <td><p>RULE_MISMATCHED_END_DATE_PERIOD_EXCEEDS</p></td>
            <td><p>Simulate scenario where the period submission end date does not match the accounting period end date.</p></td>
        </tr>
        <tr>
            <td><p>RULE_MISMATCHED_START_DATE</p></td>
            <td><p>Simulate scenario where the period submission start date must match the accounting period start date.</p></td>
        </tr>
        <tr>
            <td><p>RULE_NON_FHL_PRIVATE_USE_ADJUSTMENT</p></td>
            <td><p>Simulate scenario for UK non-Furnished Holiday Lettings, the private use adjustment must not exceed the total allowable expenses</p></td>
        </tr>
        <tr>
            <td><p>RULE_NON_MATCHING_PERIOD</p></td>
            <td><p>Simulate scenario where an End of Period Statement without a matching accounting period cannot be submitted.</p></td>
        </tr>
            <td><p>BUSINESS_EOPS_RULE_ALREADY_SUBMITTED</p></td>
            <td><p>Simulate scenario where an End of Period Statement has already been submitted for the period supplied.</p></td>
        </tr>
    </tbody>
</table>
