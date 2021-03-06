var $ = require('jquery');

String.prototype.capitalizeFirstLetter = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
};

String.prototype.escapeRegExp = function() {
    return this.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
};

module.exports = {

    transitionTo: function (route, params, activeTab) {
        var query = activeTab !== undefined ? $.extend({}, this.keyQuery(), {a: activeTab}) : this.keyQuery();
        this.context.router.transitionTo(route, params, query);
    },

    keyQuery: function () {
        var q = this.context.router.getCurrentQuery();
        return q.key !== undefined ? {key: q.key} : null;
    },

    loadData: function (url, successCallback, failCallback) {
        $.ajax({
            url: '/api/' + url,
            type: 'GET',
            dataType: 'json'
        }).done(function (data) {
            if (successCallback != null && successCallback instanceof Function) {
                successCallback(data)
            }

            if(data != null){
                this.setState(data);
            }
        }.bind(this)).fail(function (data) {
            if (failCallback != null && failCallback instanceof Function) {
                failCallback(data)
            }
        }.bind(this));
    },

    postData: function (url, data, successCallback, failCallback) {
        $.ajax({
            url: '/api/' + url,
            data: JSON.stringify(data),
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            type: 'POST'
        }).done(function (data) {
            if (successCallback != null && successCallback instanceof Function) {
                successCallback(data)
            }

            if(data != null){
                this.setState(data);
            }
        }.bind(this)).fail(function (data) {
            if (failCallback != null && failCallback instanceof Function) {
                failCallback(data)
            }
        }.bind(this));
    },

    getGenresString: function (genres){
        var result = "";
        genres.forEach(function (genre, index) {
            result = result + this.getGenreString(genre) + ((genres.length == index + 1 ) ? "" : ", ");
        }.bind(this));

        return result;
    },

    getGenreString: function (genre){
        return (genre == 'SCI_FI' ? genre.toLowerCase().replace(/_/g, '-') : genre.toLowerCase().replace(/_/g, ' '))
            .capitalizeFirstLetter();

    },

    sortListByValue: function (list, sortValue, asc) {
        if (list != null) {
            list.sort(function (a, b) {
                var v1 = a[sortValue];
                var v2 = b[sortValue];
                return asc == true ? v1 - v2 : v2 -v1;
            });
        }
    },

};