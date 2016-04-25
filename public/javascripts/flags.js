$(function() {
    $('.btn-resolve').click(function() {
        var listItem = $(this).closest('li');
        var flagId = listItem.data('flag-id');
        $.ajax({
            url: '/admin/flags/' + flagId + '/resolve/true',
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
