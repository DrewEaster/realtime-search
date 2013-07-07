'use strict';

angular.module('realtimeSearch.controllers', []).
    controller('SearchCtrl', function ($scope) {
        $scope.searchResults = [];
        $scope.searchString = "";

        $scope.addSearchResult = function (e) {
            $scope.$apply(function () {
                $scope.searchResults.unshift(JSON.parse(e.data));
            });
        }

        $scope.startSearching = function () {
            $scope.stopSearching()
            $scope.searchResults = [];
            $scope.searchFeed = new EventSource("/search/" + $scope.searchString);
            $scope.searchFeed.addEventListener("message", $scope.addSearchResult, false);
        };

        $scope.stopSearching = function () {
            if (typeof $scope.searchFeed != 'undefined') {
                $scope.searchFeed.close();
            }
        }
    });