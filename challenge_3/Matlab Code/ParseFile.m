function [status trace] = ParseFile(fid)
data = textscan(fid, '[java] SunSPOT:%s %f %f %f %f %f', 'delimiter', ',');
fields = {'ID','accelX','accelY','accelZ','tempC','tempF'};

l = length(data{1});
if (l <= 0)
   status = 2;
   trace = [];
   return
end

data{1} = cellfun(@stripID, data{1});

data = cellfun(@num2cell,data,'UniformOutput',false);

for ii = 1:length(fields)
   [trace(1:l).(fields{ii})] = deal(data{ii}{:});     
end

status = 0;
end


function dec = stripID(id)
    dec = hex2dec(id(end-3:end));
end