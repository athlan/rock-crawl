function LocalizationVisual

close all
newTrace = [];
ids = [];
Spot = [];
background = [.6 .6 .6];
f = figure('units','normalized','Position',[.2 .2 .6 .6], 'Name', 'Localization', 'Color',background, 'CloseRequestFcn', @closeReq);
dataFields = {'ID', 'Distance'};


Beacon(1).x=0;
Beacon(1).y=50;
Beacon(2).x=50;
Beacon(2).y=50;
Beacon(3).x=0;
Beacon(3).y=0;
Beacon(4).x=50;
Beacon(4).y=0;

plot(Beacon(1).x-1,Beacon(1).y,'ro');
hold on;
plot(Beacon(2).x-1,Beacon(2).y,'ro');
hold on;
plot(Beacon(3).x-1,Beacon(3).y,'ro');
hold on;
plot(Beacon(4).x-1,Beacon(4).y,'ro');
hold on;
Spot.point=plot(15,15,'g*');
 Spot.Label=text(15,15,strcat(num2str(15),',',num2str(15)));
set(Spot.point,'visible','off');
axis([-10 60 -10 60]);


for ii=1:4
       for jj = 1:length(dataFields)
        data(ii).(dataFields{jj})=[];
       end
    data(ii).length = 0;
    Beacon(ii).Distance=0;
    Beacon(ii).Label=text(Beacon(ii).x,Beacon(ii).y,int2str(ii));

end
    



set(f,'visible','on');
    
updateTimer = timer('period',.5,...
    'ExecutionMode','fixedRate',...
    'BusyMode','drop',...
    'TimerFcn',@updateData);

startButton = uicontrol('style','pushbutton',...
    'units','normalized',...
    'position',[.05 .92 .1 .05],...
    'String','Start',...
    'CallBack',@startData);

stopButton = uicontrol('style','pushbutton',...
    'units','normalized',...
    'position',[.17 .92 .1 .05],...
    'enable','off',...
    'String','Stop',...
    'CallBack',@stopData);


fid = fopen('./SpotDistance.txt');


    function closeReq(varargin)
        stop(updateTimer);
        fclose('all');
        delete(f)
    end

    function startData(varargin)
        set(startButton, 'Enable', 'off');
        set(stopButton, 'Enable', 'on');
        start(updateTimer);
    end

    function stopData(varargin)
        set(startButton, 'Enable', 'on');
        set(stopButton, 'Enable', 'off');
        stop(updateTimer);
    end

    function updateData(varargin)

        [status newTrace] = ParseFile(fid);
        % status == 0 means ok;
        % status == 1 means file open error
        % status == 2 means no update
        % status == 3 means first time to check this file.
%         disp(status);
        switch (status)
            case 1
                disp('File not Found');
                return
            case 2
                disp('No new values');
                return
            case 3
                return
        end
        
        
       
        change = zeros(1,length(ids));
        for kk = 1:length(newTrace)
            
            id = newTrace(kk).ID;
            
            index = find(id==ids);
            
            if isempty(ids) || ~(any(index))
                ids=[ids id];
                change = [change 1];
                index = length(ids);
            end
            
            for mm = 1:length(dataFields)
                
                    data(index).(dataFields{mm}) = newTrace(kk).(dataFields{mm});
                
            end
            change(index) = 1;
        end
        
        B=zeros(4,3);
        for nn=1:length(ids);
            if(change(nn)==1)
                set(Beacon(nn).Label,'String',dec2hex(data(nn).ID));
                Beacon(nn).Distance=data(nn).(dataFields{2});
            end
            B(nn,:)=[Beacon(nn).x,Beacon(nn).y,Beacon(nn).Distance];
            
        end
        if(length(ids)==4)
            [Spot.x Spot.y]=trilateration(B,4);
        end

        disp(Spot.x);
        disp(Spot.y);
         delete(Spot.point);
         delete(Spot.Label);
        Spot.point=plot(Spot.x,Spot.y,'g*');
        %plot(Spot.x,Spot.y,'g*');
        %hold on;
        Spot.Label=text(Spot.x+1,Spot.y,strcat(num2str(Spot.x),',',num2str(Spot.y)));
        
    end
end
