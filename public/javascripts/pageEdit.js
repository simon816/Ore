var onNewPage = function(pageName) {
};

$(function() {
    $('#continue-page').click(function() {
        onNewPage($('#page-name').val())
    })
});
