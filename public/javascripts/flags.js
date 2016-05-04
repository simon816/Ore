function addSpinner(e) {
    return e.addClass('fa-spinner fa-spin');
}

function removeSpinner(e) {
    return e.removeClass('fa-spinner fa-spin');
}

$(function() {
    var resolveAll = $('.btn-resolve-all');
    resolveAll.click(function() {
        $('.btn-resolve').click();
        $(this).fadeOut();
    });

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
                        resolveAll.fadeOut();
                        $('.no-flags').fadeIn();
                        clearUnread($('a[href="/admin/flags"]'));
                    }
                });
            }
        });
    });
});
