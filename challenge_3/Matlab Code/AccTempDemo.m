%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% This is a simple demo to analyze coming data files quasi real time
% check the length of the data file, if new data come in, plot them on
% screen
% author:
% Yuting Zhang 09/19/2012
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

clc;clear;close all;
period = 1/5; % P
% Store the Last line. Find the Content that is appended. And Plot them
oldLen = 0;
newTrace = [];
IDSet = []; % Set of Node ID
stateSet = [];
count = 0; % initialize data count

num = 0;
while 1
    num = num + 1;
    %disp('check file length')
    [status, oldLen, newTrace]= CheckFile('./SPOTAccelTempData2.txt', oldLen);
    % status == 0 means ok;
    % status == 1 means file open error
    % status == 2 means no update
    % status == 3 means first time to check this file.
    pause(period)
    if status == 1
        disp('cannot find the file...')
        continue;
    elseif status == 2
        %disp('there is no update')
        continue;
    elseif status == 3
        continue;
    end
    disp('there is update')
    
    allID = [newTrace.ID];
    uSet = unique(allID); % Get the unique ID that has sent data.
    newID = setdiff(uSet, IDSet); % Get new ID that just start started
    IDSet = [IDSet, newID]; % Update ID set
    stateSet{ length(IDSet) } = []; % Initialize the state for NewID.
    
    disp(['Length of NewTrace: ', num2str(length(newTrace))])
    for i = 1:length(newTrace)
        id = newTrace(i).ID; % Get the source ID of this data gram.
        Acc = [newTrace(i).accelX, newTrace(i).accelY, newTrace(i).accelZ];
        Temp = [newTrace(i).tempC, newTrace(i).tempF];
        count = count+1;
        disp(['Coming data updated: ',num2str(count)]),
        disp(newTrace(1,i));
               
      
    end
end