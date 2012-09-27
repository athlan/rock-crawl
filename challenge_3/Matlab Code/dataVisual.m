function dataVisual

close all
newTrace = [];
ids = [];
firstTime = 1;

background = [.6 .6 .6];
f = figure('units','normalized','Position',[.2 .2 .6 .6], 'Color',background, 'CloseRequestFcn', @closeReq);
dataFields = {'accelX' 'accelY', 'accelZ', 'tempC', 'tempF'};

for ii = 3:-1:1
    handles(ii).temp = axes('Position',[.05 .3*ii-.25 .4 .2],...
        'parent',f,...
        'NextPlot', 'add',...
        'YLim',[0 120],...
        'YLimMode','manual',...
        'XLim', [1 50],...
        'XLimMode','manual');
    handles(ii).accel = axes('Position',[.55 .3*ii-.25 .4 .2],...
        'parent',f,...
        'NextPlot', 'add',...
        'YLim',[-4 4],...
        'YLimMode','manual',...
        'XLim', [1 50],...
        'XLimMode','manual');
    idTitles(ii) = uicontrol(...
        'style', 'text',...
        'Units','normalized',...
        'Position',[.4 .3*ii-.05 .2 .05],...
        'String', '',...
        'BackGroundColor',background,...
        'FontUnits','Normalized',...
        'FontSize',1);
    for jj = 1:length(dataFields)
        data(ii).(dataFields{jj})=[];
    end
    data(ii).length = 0;
    
    
    plots(ii).tempF = area(handles(ii).temp, 0);
    set(plots(ii).tempF, 'LineWidth',2,'FaceColor',[1 0 0]);
    plots(ii).tempC = area(handles(ii).temp, 0);
    set(plots(ii).tempC, 'LineWidth',2,'FaceColor',[0 0 1]);
    title(handles(ii).temp, 'Temperature');
    legend(handles(ii).temp, 'Temp F', 'Temp C', 'Location', 'NorthWest')
    
    
    
    plots(ii).accelX = line(0, 0, 'parent', handles(ii).accel, 'color', [0 0 1]);
    plots(ii).accelY = line(0, 0, 'parent', handles(ii).accel, 'color', [0 1 0]);
    plots(ii).accelZ = line(0, 0, 'parent', handles(ii).accel, 'color', [1 0 0]);
    title(handles(ii).accel, 'Acceleration');
    legend(handles(ii).accel, 'Accel X', 'Accel Y', 'Accel Z', 'Location', 'NorthWest');
end

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

history = uicontrol('style','pushbutton',...
    'units','normalized',...
    'position',[.29 .92 .1 .05],...
    'String','History',...
    'CallBack',@showHistory);

fid = fopen('./SpotAccelTempData2.txt');

set(f,'visible','on')

    function closeReq(varargin)
        fclose('all');
        delete(f)
    end

    function startData(varargin)
        set(startButton, 'Enable', 'off');
        set(stopButton, 'Enable', 'on');
        set(history,'enable','off')
        start(updateTimer);
    end

    function stopData(varargin)
        set(startButton, 'Enable', 'on');
        set(stopButton, 'Enable', 'off');
        stop(updateTimer);
    end

    function showHistory(varargin)
        set(startButton, 'Enable', 'off');
        [status newTrace] = ParseFile(fid);
        if(status ==2)
            disp('No Information')
            return
        end
        
        if firstTime == 1
            ids = unique([newTrace.ID]);
            firstTime = 0;
        end
        for pp = 1:length(ids)
            set([handles(pp).temp, handles(pp).accel], 'XLimMode', 'auto', 'YLimMode', 'auto')
            set(idTitles(pp), 'String', dec2hex(ids(pp)));
            matches = (ids(pp) == [newTrace.ID]);
            for field = 1:length(dataFields)
                data(pp).(dataFields{field}) = [data(pp).(dataFields{field}) [newTrace(matches).(dataFields{field})]];
                set(plots(pp).(dataFields{field}),...
                    'XData', 1:length(data(pp).(dataFields{field})),...
                    'YData', data(pp).(dataFields{field}));
            end
        end
    end

    function updateData(varargin)
        
        %[status, oldLen, newTrace] = CheckFile('./SpotAccelTempData2.txt', oldLen');
        [status newTrace] = ParseFile(fid);
        % status == 0 means ok;
        % status == 1 means file open error
        % status == 2 means no update
        % status == 3 means first time to check this file.
        switch (status)
            case 1
                disp('File not Found');
                return
            case 2
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
                set(idTitles(index), 'String', dec2hex(id));
            end
            
            for mm = 1:length(dataFields)
                if(data(index).length < 50)
                    data(index).(dataFields{mm}) = [data(index).(dataFields{mm}) newTrace(kk).(dataFields{mm})];
                    if (mm == length(dataFields))
                        data(index).length = data(index).length + 1;
                    end
                else
                    data(index).(dataFields{mm}) = [data(index).(dataFields{mm})(2:end) newTrace(kk).(dataFields{mm})];
                end
            end
            change(index) = 1;
        end
        
        
        for nn = 1:length(ids)
            if change(nn) == 1
                for oo = 1:length(dataFields)
                    set(plots(nn).(dataFields{oo}),...
                        'XData', 1:data(nn).length,...
                        'YData', data(nn).(dataFields{oo}));
                end
            end
        end
        
        
    end
end
