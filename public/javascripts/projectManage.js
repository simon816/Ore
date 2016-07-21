var projectName = null;
var PROJECT_OWNER = null;
var PROJECT_SLUG = null;

$(function() {
    $('#name').on('input', function() {
        var val = $(this).val();
        $('#btn-rename').prop('disabled', val.length === 0 || val === projectName);
    });
});
