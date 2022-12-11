package com.nickperov.stud.micro_notes_spring_boot.controller;

import com.nickperov.stud.micro_notes_spring_boot.api.NoteDTO;
import com.nickperov.stud.micro_notes_spring_boot.service.NotesService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/notes")
public class NotesController {

    private final NotesService notesService;

    @Autowired
    private NotesController(final NotesService notesService) {
        this.notesService = notesService;
    }

    @GetMapping()
    public List<NoteDTO> listNotes() {
        return notesService.getAllNotes().stream().map(NoteDTO::new).collect(Collectors.toList());
    }

    @GetMapping("/{note_id}")
    public ResponseEntity<NoteDTO> getNote(@PathVariable("note_id") final UUID noteId) {
        final var note = notesService.getNote(noteId);
        if (note != null) {
            return ResponseEntity.ok(new NoteDTO(note));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping()
    public NoteDTO createNote(@RequestBody String noteText) {
        return new NoteDTO(notesService.createNote(noteText));
    }

    @PutMapping("/{note_id}")
    public ResponseEntity<?> updateNote(@PathVariable("note_id") final UUID noteId, @RequestBody NoteDTO note) {
        if (note.getId() == null || !note.getId().equals(noteId) || note.getText() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        final boolean isUpdated = notesService.updateNote(note);
        if (isUpdated) {
            return ResponseEntity.ok().build();
        } else {
            // TODO return URL of created note 
            final URI uri = ServletUriComponentsBuilder.fromCurrentRequestUri().port(8080).build().toUri();
            return ResponseEntity.created(uri).build();
        }
    }

    @DeleteMapping("/{note_id}")
    public ResponseEntity<?> deleteNote(@PathVariable("note_id") final UUID noteId) {
        final var result = notesService.deleteNote(noteId);
        return result ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
