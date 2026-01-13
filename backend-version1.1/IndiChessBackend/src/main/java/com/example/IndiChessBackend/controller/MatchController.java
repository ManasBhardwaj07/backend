package com.example.IndiChessBackend.controller;

import com.example.IndiChessBackend.model.Move;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/game")
public class MatchController {

//    @RequestMapping("")
//    public Optional<?> submitMatch(){
//        return Optional.of("Match Started");
//    }


    @RequestMapping("")
    public Optional<?> submitMove( @RequestBody Move move){
        System.out.println(move);
        return Optional.of("Move stored in db");
    }


}
