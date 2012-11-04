function [x, y] = trilateration(B, BeaconN)
% 2D Trilateration to determine the location of the unknoown node
% Syntax: 
% B - Beacon matrix, 
%       B(i,:) represents a beacon, 
%       B(i,1) is the x coordinate of Beacon i,
%       B(i,2) is the y coordinate of Beacon i,
%       B(i,3) is the d(istance) from the unknown node to Beacon i.
% BeaconN - Number of Beacons 
% [x, y] - the x, y coordinates of the unknown node
% algorithm: 
% Q = inv(D'*D)*D'*b
% where D = 2* [x1- x2, y1-y2; x1-x3, y1-y3; ...; x1-xn, y1-yn]
%       b = [x1^2-x2^2+y1^2-y2^2+d1^2-d2^2;...;x1^2-xn^2+y1^2-yn^2+d1^2-dn^2]
% Q = [x;y] the coordinates of the unknown node.
% Author:
% Yuting Zhang <ytzhang@bu.edu>
% 10/24/2012

D = zeros(BeaconN -1, 2);
b = zeros(BeaconN -1, 1);
for i = 1 : BeaconN -1
    D(i, :) = [ B(1,1) - B(i+1, 1), B(1, 2) - B(i+1, 2) ];
    b(i) = B(1,1)^2 - B(i+1,1)^2 + B(1,2)^2 - B(i+1,2)^2 - B(1,3)^2 + B(i+1,3)^2;
end
D = 2 * D;
DT = D';
Q = (DT * D) \ DT * b;
x = Q(1);
y = Q(2);