$(function() {
    $('.btn-resolve').click(function() {
        var listItem = $(this).closest('li');
        var flagId = listItem.data('flag-id');
        $.ajax({
            url: '/admin/flags/' + flagId + '/resolve/true',
            success: function() { listItem.fadeOut('slow'); }
        });
    });
});
