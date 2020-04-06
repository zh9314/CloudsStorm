
Cloud Performance Test 
=================================================

This folder contains the example code and results of cloud performance test.

Code Example
-------------------------
All the necessary files for codes part are in the folder of **App/** and **Infs/**.

:**App/**:
    This folder contains user-developed applications. 
    The file "infrasCode.yml" is the infrastructure code. It specifies how to provision and control the infrastructure.

:**Infs/**:
    This folder contains all the application-defined infrastructure descriptions. It contains following subfolders. 
    
    :**-> Topology/**: 
        It contains all the topology descriptions for this application. There are two levels of descriptions. 

        :**-> _top.yml**: 
            This is the top-level description. 
            
            It describes how different sub-topologies from different data centers are connected. 

        :**-> UC/**: 
            This folder contains all the user's necessary credential information to access these Clouds.

        :**-> UD/**: 
            This folder contains all the necessary information for these Clouds. They work as a knowledge base. 

  
Results
----------------
In this case, all the results are recorded in the Log file, which is also in YAML format.

:**Logs/**:
    File "InfrasCode.log" contains all the recorded logs. It records all the provisioning overhead and command outputs defined in previous infrastructure code, e.g. "infrasCode.yml". 