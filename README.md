- You have successfully created an Atlassian Plugin!

Here are the SDK commands you'll use immediately:

- atlas-run # installs this plugin into the product and starts it on localhost
- atlas-debug # same as atlas-run, but allows a debugger to attach at port 5005
- atlas-cli # after atlas-run or atlas-debug, opens a Maven command line window:
   - 'pi' reinstalls the plugin into the running product instance
                 
- atlas-help # prints description for all commands in the SDK

<br>

<div align="center">

Full documentation [AVAILABLE HERE](https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK)
</div>
<br>

<details>
<summary> <b> STEPS TO IMPLEMENT JIRA TIMESHEET </b> </summary>

## Step 1: Enable Time Tracking in JIRA

<br>
JIRA timesheet entry is enabled by default.

Timesheet configuration can be changed to meet your need by going to the Administration Gear > Issues > Time tracking page. The [Atlassian support page](https://confluence.atlassian.com/adminjiraserver/configuring-time-tracking-938847808.html) explains these settings in detail.

<br>

## Step 2: Prepare your why and explain it to your team

<br>
Explain why you are asking your team to enter the timesheet. There could be multiple reasons for time entry. <u> Ex </u>: using timesheet data to compare estimated vs actual time along with checking project health. <u> Capacity planning </u> is another big reason how organizations use timesheet data.

<br>

## Step 3: Prepare guidelines based on your workflow and answer the situations your team will encounter
<br>
It may include details like how to track meetings/admin time and post-production bug fix (operational) time within sprints and recommended structures for epics.

<br>

## Step 4: Generating timesheet reports.
There are a few options to generate timesheet reports.

- Option 1 : JIRA has a “Time Tracking Report” for each project, but this report shows basic data. We needed time data across teams.

- Option 2 : Use an add-on such as Tampo, which generates nice reporting and enhances time entry screens.

- Option 3 : Create a search filter with selected projects or selected team members. Include tickets updated during a certain time period and export this data into an excel file. There you can create a pilot table-based report.

   - Problem — This method combines time entry by multiple people at tasks/sub-task levels.

<br>

- Option 4 : Use JIRA backend data along with PowerBi. This is an Advance method that we used. This requires a specialized person to understand the JIRA database structure and build a PowerBi report using that data.

</details>

<br><br>

## Contributors 

<br>

<a href="https://github.com/Catrobat/Timesheet/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Catrobat/Timesheet" />
</a>


