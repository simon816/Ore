function init() {
    // Display description
    $('#description').html($('#description-view').html());

    // Setup edit button
    $('#description-edit').click(function() {
        // Replace description with editor
        $('#description').html($("#editor").html());

        // Enable tabs
        $('[data-toggle="tab"]').click(function(event) {
            event.preventDefault();
            $(this).tab('show');
        });

        // Render preview
        $('[href="#preview"]').on('shown.bs.tab', function() {
            $("#preview").html(markdown.toHTML($("#version-editor").val()));
        });

        // Save description
        $('#description-save').click(function() {
            event.preventDefault();
            $('#form-save').submit();
        });

        // Cancel editor
        $('#description-cancel').click(function() {
            event.preventDefault();
            init();
        });
    });
}

$(function() {
    init();
});
