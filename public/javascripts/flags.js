function addSpinner(e) {
    return e.addClass('fa-spinner fa-spin');
}

function removeSpinner(e) {
    return e.removeClass('fa-spinner fa-spin');
}

$(function() {
    $('.btn-resolve').click(function() {
        var listItem = $(this).closest('li');
        var flagId = listItem.data('flag-id');
        var spinner = addSpinner($(this).find('i').removeClass('fa-check'));
        $.ajax({
            url: '/admin/flags/' + flagId + '/resolve/true',
            complete: function() { removeSpinner(spinner.addClass('fa-check')); },
            success: function() {
                $.when(listItem.fadeOut('slow')).done(function() {
                    listItem.remove();
                    if (!$('.list-flags-admin').find('li').length) {
                        $('.unread').remove();
                        $('.no-flags').fadeIn();
                    }
                });
            }
        });
    });
});
