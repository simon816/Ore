var projectName = null;

$(function() {
    $('#name').on('input', function() {
        var val = $(this).val();
        $('#btn-rename').prop('disabled', val.length === 0 || val === projectName);
    });
});
