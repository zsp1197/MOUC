
Brief description:

The unit commitment program can deal with the following constraints:
1. Load balance
2. min on/off time
3. start up cost
4. linear less than and equal to constraints


How to use:
1. Before you run the program, java runtime enviroment(JRE) are needed. Please search and install the newest JRE.
2. UC-context.xml is the configure file. You can specify the algorithm to solve the economid dispatch problem and gap in the algorithm.
3. Example loads and reserve requirement data are in Loads.xml file. You can change the file name but make sure it is changed in the UC-context.xml also.
4. Example generator data are in Generator*.xml file.
5. Example less than and equal to constraints are in LeqCosntratins.xml file.
   less than and equal to constraints are constraints with the generators' power level:
   sigma_i a_i * p_i <= b  , \forany t
6. double click "kcmd.bat", then input "run" in the command window, you will see the process information in the window, or you could check the scucAlg.log file to see them.

Jiachun GUO
April 23, 2007