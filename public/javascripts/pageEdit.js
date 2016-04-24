var KEY_ENTER = 13;

var onNewPage = function(pageName) {
};

$(function() {
    var modal = $('#new-page');
    modal.on('shown.bs.modal', function() {
        $(this).find('input').focus();
    });

    modal.find('input').keydown(function(event) {
        if (event.keyCode === KEY_ENTER) {
            event.preventDefault();
            $('#continue-page').click();
        }
    });

    $('#continue-page').click(function() {
        onNewPage($('#page-name').val())
    })
});
