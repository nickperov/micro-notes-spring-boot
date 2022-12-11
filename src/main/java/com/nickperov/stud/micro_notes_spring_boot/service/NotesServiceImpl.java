package com.nickperov.stud.micro_notes_spring_boot.service;

import com.nickperov.stud.micro_notes_spring_boot.api.Note;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class NotesServiceImpl implements com.nickperov.stud.micro_notes_spring_boot.service.NotesService {

    private final Map<UUID, NoteImpl> notes = new ConcurrentHashMap<>();

    @Override
    public Note getNote(final UUID id) {
        return notes.get(id);
    }

    @Override
    public List<? extends Note> getAllNotes() {
        return notes.values().stream().sorted(Comparator.comparing(NoteImpl::getTimestamp).reversed()).toList();
    }

    @Override
    public Note createNote(final String text) {
        final NoteImpl note = new NoteImpl(text);
        notes.put(note.getId(), note);
        return note;
    }

    @Override
    public boolean updateNote(final Note note) {
        final NoteImpl newNote = new NoteImpl(note);
        return notes.put(newNote.getId(), newNote) != null;
    }

    @Override
    public boolean deleteNote(final UUID id) {
        return notes.remove(id) != null;
    }
    
    public void cleanUp() {
        notes.clear();
    }
}
