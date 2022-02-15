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
            <td><p>NOT_FOUND</p></td>
            <td><p>Simulate scenario where no data found.</p></td>
        </tr>
        <tr>
            <td><p>ALREADY_SUBMITTED</p></td>
            <td><p>Simulate scenario where the user has previously submitted an End of Period Statement for this business' accounting period.</p></td>
        </tr>
        <tr>
            <td><p>CLASS4_OVER_16</p></td>
            <td><p>Simulate scenario where the user has requested a National Insurance Class 4 exemption but the individual’s age is greater than or equal to 16 years old on the 6th April of the current tax year.</p></td>
        </tr>
        <tr>
            <td><p>CLASS4_PENSION_AGE</p></td>
            <td><p>Simulate scenario where the user has requested a National Insurance Class 4 exemption but the individual's age is less than their State Pension age on the 6th April of the current tax year.</p></td>
        </tr>
        <tr>
            <td><p>CONSOLIDATED_EXPENSES</p></td>
            <td><p>Simulate scenario where consolidated expenses are not allowed if the cumulative turnover amount exceeds the threshold.</p></td>
        </tr>
        <tr>
            <td><p>EARLY_SUBMISSION</p></td>
            <td><p>Simulate scenario where the user has tried to make their End of Period Statement declaration before the accounting period has ended.</p></td>
        </tr>
        <tr>
            <td><p>LATE_SUBMISSION</p></td>
            <td><p>Simulate scenario where the user has tried to make their End of Period Statement declaration too late.</p></td>
        </tr>
        <tr>
            <td><p>MISMATCHED_START_DATE</p></td>
            <td><p>Simulate scenario where the period submission start date does not match the accounting period start date.</p></td>
        </tr>
        <tr>
            <td><p>MISMATCHED_END_DATE</p></td>
            <td><p>Simulate scenario where the period submission end date does not match the accounting period end date.</p></td>
        </tr>
        <tr>
            <td><p>FHL_PRIVATE_USE_ADJUSTMENT</p></td>
            <td><p>Simulate scenario for UK Furnished Holiday Lettings, the private use adjustment exceeds the total allowable expenses.</p></td>
        </tr>
        <tr>
            <td><p>NON_FHL_PRIVATE_USE_ADJUSTMENT</p></td>
            <td><p>Simulate scenario for UK non-Furnished Holiday Lettings, the private use adjustment exceeds the total allowable expenses.</p></td>
        </tr>
        <tr>
            <td><p>NON_MATCHING_PERIOD</p></td>
            <td><p>Simulate scenario where an End of Period Statement without a matching accounting period cannot be submitted.</p></td>
        </tr>
    </tbody>
</table>