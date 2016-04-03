var baseUrl = null;
var projectOwner = null;
var projectName = null;
var alreadyStarred = false;

$(function() {

    // setup star button
    var increment = alreadyStarred ? -1 : 1;
    $(".btn-star").click(function() {
        var starred = $(this).find(".starred");
        starred.html(" " + (parseInt(starred.text()) + increment).toString());
        $.ajax(baseUrl + '/' + projectOwner + '/' + projectName + "/star/" + (increment > 0));
        increment *= -1;
    });

});
